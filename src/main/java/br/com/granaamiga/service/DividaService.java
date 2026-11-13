package br.com.granaamiga.service;

import br.com.granaamiga.model.AlertaVencimentoResponse;
import br.com.granaamiga.model.Divida;
import br.com.granaamiga.model.DividaRequest;
import br.com.granaamiga.model.DividaResponse;
import br.com.granaamiga.model.FaixaComprometimento;
import br.com.granaamiga.model.ParcelaPlanoResponse;
import br.com.granaamiga.model.PlanoQuitacaoResponse;
import br.com.granaamiga.model.StatusDivida;
import br.com.granaamiga.model.TermometroDividasResponse;
import br.com.granaamiga.model.Usuario;
import br.com.granaamiga.repository.DividaRepository;
import br.com.granaamiga.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class DividaService {

    private static final int MAX_MESES = 600;
    private static final int MAX_PARCELAS_VISUAIS = 12;
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final DividaRepository dividas;
    private final UsuarioRepository usuarios;
    private final Clock clock;

    public DividaService(DividaRepository dividas, UsuarioRepository usuarios, Clock clock) {
        this.dividas = dividas;
        this.usuarios = usuarios;
        this.clock = clock;
    }

    public List<DividaResponse> listar(Long usuarioId) {
        return dividas.findByUsuarioId(usuarioId).stream().map(DividaResponse::from).toList();
    }

    @Transactional
    public DividaResponse criar(Long usuarioId, DividaRequest request) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
        Divida divida = new Divida();
        divida.setUsuario(usuario);
        aplicar(request, divida);
        divida.setStatus(request.status() == null ? StatusDivida.ABERTA : request.status());
        return DividaResponse.from(dividas.save(divida));
    }

    public DividaResponse buscar(Long usuarioId, Long id) {
        return DividaResponse.from(buscarDoUsuario(usuarioId, id));
    }

    @Transactional
    public DividaResponse atualizar(Long usuarioId, Long id, DividaRequest request) {
        Divida divida = buscarDoUsuario(usuarioId, id);
        aplicar(request, divida);
        if (request.status() != null) {
            divida.setStatus(request.status());
        }
        return DividaResponse.from(dividas.save(divida));
    }

    @Transactional
    public void excluir(Long usuarioId, Long id) {
        dividas.delete(buscarDoUsuario(usuarioId, id));
    }

    public TermometroDividasResponse termometro(Long usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
        BigDecimal totalParcelas = dividas.findByUsuarioId(usuarioId).stream()
                .filter(divida -> divida.getStatus() == StatusDivida.ABERTA)
                .map(Divida::getValorParcela)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        BigDecimal renda = usuario.getRendaMensal();
        if (renda == null || renda.signum() == 0) {
            return new TermometroDividasResponse(renda, totalParcelas, null,
                    FaixaComprometimento.SEM_RENDA);
        }
        FaixaComprometimento faixa = totalParcelas.compareTo(renda.multiply(new BigDecimal("0.30"))) <= 0
                ? FaixaComprometimento.VERDE
                : totalParcelas.compareTo(renda.multiply(new BigDecimal("0.50"))) <= 0
                        ? FaixaComprometimento.AMARELO : FaixaComprometimento.VERMELHO;
        BigDecimal percentual = totalParcelas.multiply(CEM).divide(renda, 2, RoundingMode.HALF_UP);
        return new TermometroDividasResponse(renda, totalParcelas, percentual, faixa);
    }

    public List<AlertaVencimentoResponse> alertas(Long usuarioId, LocalDate dataReferencia, int dias) {
        if (dias < 0 || dias > 365) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dias deve estar entre 0 e 365");
        }
        LocalDate referencia = dataReferencia == null ? LocalDate.now(clock) : dataReferencia;
        return dividas.findByUsuarioId(usuarioId).stream()
                .filter(divida -> divida.getStatus() == StatusDivida.ABERTA)
                .map(divida -> alerta(divida, referencia))
                .filter(alerta -> alerta.diasRestantes() <= dias)
                .sorted(Comparator.comparing(AlertaVencimentoResponse::dataVencimento)
                        .thenComparing(AlertaVencimentoResponse::credor)
                        .thenComparing(AlertaVencimentoResponse::dividaId))
                .toList();
    }

    private AlertaVencimentoResponse alerta(Divida divida, LocalDate referencia) {
        YearMonth mes = YearMonth.from(referencia);
        LocalDate vencimento = mes.atDay(Math.min(divida.getDiaVencimento(), mes.lengthOfMonth()));
        if (vencimento.isBefore(referencia)) {
            mes = mes.plusMonths(1);
            vencimento = mes.atDay(Math.min(divida.getDiaVencimento(), mes.lengthOfMonth()));
        }
        long diasRestantes = ChronoUnit.DAYS.between(referencia, vencimento);
        return new AlertaVencimentoResponse(divida.getId(), divida.getCredor(),
                divida.getValorParcela(), vencimento, diasRestantes, diasRestantes <= 7);
    }

    public PlanoQuitacaoResponse plano(Long usuarioId, Long id) {
        Divida divida = buscarDoUsuario(usuarioId, id);
        BigDecimal saldoInicial = divida.getSaldoDevedor().setScale(2, RoundingMode.HALF_UP);
        BigDecimal saldo = saldoInicial;
        BigDecimal taxa = divida.getTaxaJurosMes();
        BigDecimal parcelaFixa = divida.getValorParcela().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPago = BigDecimal.ZERO.setScale(2);
        BigDecimal totalJuros = BigDecimal.ZERO.setScale(2);
        List<ParcelaPlanoResponse> primeirasParcelas = new ArrayList<>(MAX_PARCELAS_VISUAIS);

        if (saldo.signum() <= 0) {
            return respostaPlano(divida, true, "Dívida já quitada.", 0, 0, saldoInicial, taxa,
                    parcelaFixa, totalPago, totalJuros, saldo, primeirasParcelas);
        }

        BigDecimal jurosIniciais = jurosDoMes(saldo, taxa);
        if (parcelaFixa.compareTo(jurosIniciais) <= 0) {
            // Mostra só o começo da trajetória; não projeta indefinidamente uma dívida crescente.
            for (int mes = 1; mes <= MAX_PARCELAS_VISUAIS; mes++) {
                ParcelaPlanoResponse etapa = calcularParcela(mes, saldo, taxa, parcelaFixa);
                primeirasParcelas.add(etapa);
                totalPago = totalPago.add(etapa.pagamento());
                totalJuros = totalJuros.add(etapa.juros());
                saldo = etapa.saldoFinal();
            }
            return respostaPlano(divida, false, "A parcela não supera os juros mensais; a dívida não quita com esse valor.",
                    null, MAX_PARCELAS_VISUAIS, saldoInicial, taxa, parcelaFixa,
                    totalPago, totalJuros, saldo, primeirasParcelas);
        }

        for (int mes = 1; mes <= MAX_MESES; mes++) {
            ParcelaPlanoResponse etapa = calcularParcela(mes, saldo, taxa, parcelaFixa);
            if (primeirasParcelas.size() < MAX_PARCELAS_VISUAIS) {
                primeirasParcelas.add(etapa);
            }
            totalPago = totalPago.add(etapa.pagamento());
            totalJuros = totalJuros.add(etapa.juros());
            saldo = etapa.saldoFinal();
            if (saldo.signum() == 0) {
                return respostaPlano(divida, true, "Quitação estimada com a parcela informada.", mes, mes,
                        saldoInicial, taxa, parcelaFixa, totalPago, totalJuros, saldo, primeirasParcelas);
            }
        }

        return respostaPlano(divida, false, "A dívida não quita em até 600 meses com a parcela informada.",
                null, MAX_MESES, saldoInicial, taxa, parcelaFixa,
                totalPago, totalJuros, saldo, primeirasParcelas);
    }

    private Divida buscarDoUsuario(Long usuarioId, Long id) {
        Divida divida = dividas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dívida não encontrada"));
        if (!divida.getUsuario().getId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dívida não encontrada");
        }
        return divida;
    }

    private void aplicar(DividaRequest request, Divida divida) {
        StatusDivida statusFinal = request.status() != null ? request.status()
                : divida.getStatus() == null ? StatusDivida.ABERTA : divida.getStatus();
        if ((statusFinal == StatusDivida.QUITADA && request.saldoDevedor().signum() != 0)
                || (statusFinal == StatusDivida.ABERTA && request.saldoDevedor().signum() == 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dívida quitada deve ter saldo zero; dívida aberta deve ter saldo positivo");
        }
        divida.setCredor(request.credor().trim());
        divida.setTipo(request.tipo());
        divida.setSaldoDevedor(request.saldoDevedor());
        divida.setTaxaJurosMes(request.taxaJurosMes());
        divida.setValorParcela(request.valorParcela());
        divida.setDiaVencimento(request.diaVencimento());
    }

    private BigDecimal jurosDoMes(BigDecimal saldo, BigDecimal taxaPercentual) {
        return saldo.multiply(taxaPercentual).divide(CEM, 2, RoundingMode.HALF_UP);
    }

    private ParcelaPlanoResponse calcularParcela(int mes, BigDecimal saldo, BigDecimal taxa, BigDecimal parcelaFixa) {
        BigDecimal juros = jurosDoMes(saldo, taxa);
        BigDecimal devido = saldo.add(juros);
        BigDecimal pagamento = parcelaFixa.min(devido);
        BigDecimal amortizacao = pagamento.subtract(juros);
        BigDecimal saldoFinal = devido.subtract(pagamento).max(BigDecimal.ZERO.setScale(2));
        return new ParcelaPlanoResponse(mes, saldo, juros, pagamento, amortizacao, saldoFinal);
    }

    private PlanoQuitacaoResponse respostaPlano(
            Divida divida, boolean quitavel, String aviso, Integer mesesParaQuitacao, int mesesSimulados,
            BigDecimal saldoInicial, BigDecimal taxa, BigDecimal parcelaFixa, BigDecimal totalPago,
            BigDecimal totalJuros, BigDecimal saldoRestante, List<ParcelaPlanoResponse> primeirasParcelas
    ) {
        return new PlanoQuitacaoResponse(divida.getId(), quitavel, aviso, mesesParaQuitacao, mesesSimulados,
                saldoInicial, taxa, parcelaFixa, totalPago, totalJuros, saldoRestante, List.copyOf(primeirasParcelas));
    }
}
