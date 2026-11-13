package br.com.granaamiga.service;

import br.com.granaamiga.model.DispositivoDto;
import br.com.granaamiga.model.DispositivoForm;
import br.com.granaamiga.model.FaixaEnergia;
import br.com.granaamiga.model.LeituraEnergiaDto;
import br.com.granaamiga.model.LeituraEnergiaForm;
import br.com.granaamiga.model.ResumoEnergiaDto;
import br.com.granaamiga.model.Dispositivo;
import br.com.granaamiga.model.LeituraEnergia;
import br.com.granaamiga.model.Usuario;
import br.com.granaamiga.repository.DispositivoRepository;
import br.com.granaamiga.repository.LeituraEnergiaRepository;
import br.com.granaamiga.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EnergiaService {

    private final DispositivoRepository dispositivos;
    private final LeituraEnergiaRepository leituras;
    private final UsuarioRepository usuarios;
    private final Clock clock;

    public EnergiaService(DispositivoRepository dispositivos, LeituraEnergiaRepository leituras,
            UsuarioRepository usuarios, Clock clock) {
        this.dispositivos = dispositivos;
        this.leituras = leituras;
        this.usuarios = usuarios;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<DispositivoDto> listarDispositivos(Long usuarioId) {
        return dispositivos.findByUsuarioId(usuarioId).stream().map(DispositivoDto::from).toList();
    }

    @Transactional(readOnly = true)
    public DispositivoDto buscarDispositivo(Long usuarioId, Long dispositivoId) {
        return DispositivoDto.from(dispositivoDoUsuario(usuarioId, dispositivoId));
    }

    @Transactional
    public DispositivoDto criarDispositivo(Long usuarioId, DispositivoForm form) {
        String codigo = form.codigo().trim();
        validarCodigoDisponivel(codigo, null);
        Usuario usuario = usuarios.findById(usuarioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        Dispositivo dispositivo = new Dispositivo();
        dispositivo.setUsuario(usuario);
        preencherDispositivo(dispositivo, form, codigo);
        return DispositivoDto.from(dispositivos.save(dispositivo));
    }

    @Transactional
    public DispositivoDto atualizarDispositivo(Long usuarioId, Long dispositivoId, DispositivoForm form) {
        Dispositivo dispositivo = dispositivoDoUsuario(usuarioId, dispositivoId);
        String codigo = form.codigo().trim();
        validarCodigoDisponivel(codigo, dispositivoId);
        preencherDispositivo(dispositivo, form, codigo);
        return DispositivoDto.from(dispositivos.save(dispositivo));
    }

    @Transactional
    public void excluirDispositivo(Long usuarioId, Long dispositivoId) {
        Dispositivo dispositivo = dispositivoDoUsuario(usuarioId, dispositivoId);
        if (leituras.existsByDispositivoId(dispositivoId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dispositivo com leituras não pode ser excluído");
        }
        dispositivos.delete(dispositivo);
    }

    @Transactional(readOnly = true)
    public Page<LeituraEnergiaDto> listarLeituras(
            Long usuarioId, Long dispositivoId, int pagina, int tamanho) {
        dispositivoDoUsuario(usuarioId, dispositivoId);
        if (pagina < 0 || tamanho < 1 || tamanho > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Página deve ser >= 0 e tamanho deve estar entre 1 e 100");
        }
        return leituras.findByDispositivoIdOrderByDataLeituraDescIdDesc(
                dispositivoId, PageRequest.of(pagina, tamanho)).map(LeituraEnergiaDto::from);
    }

    @Transactional(readOnly = true)
    public LeituraEnergiaDto buscarLeitura(Long usuarioId, Long dispositivoId, Long leituraId) {
        dispositivoDoUsuario(usuarioId, dispositivoId);
        LeituraEnergia leitura = leituras.findById(leituraId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Leitura não encontrada"));
        if (!dispositivoId.equals(leitura.getDispositivo().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leitura não encontrada");
        }
        return LeituraEnergiaDto.from(leitura);
    }

    @Transactional
    public LeituraEnergiaDto registrarLeitura(Long usuarioId, Long dispositivoId, LeituraEnergiaForm form) {
        Dispositivo dispositivo = dispositivoDoUsuario(usuarioId, dispositivoId);
        if (form.dataLeitura() != null && form.dataLeitura().isAfter(LocalDateTime.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leitura não pode estar no futuro");
        }
        LeituraEnergia leitura = new LeituraEnergia();
        leitura.setDispositivo(dispositivo);
        leitura.setDataLeitura(form.dataLeitura() == null ? LocalDateTime.now(clock) : form.dataLeitura());
        leitura.setPotenciaW(form.potenciaW());
        leitura.setKwh(form.kwh());
        leitura.setCustoEstimado(form.kwh().multiply(dispositivo.getTarifaKwh())
                .setScale(2, RoundingMode.HALF_UP));
        return LeituraEnergiaDto.from(leituras.save(leitura));
    }

    @Transactional(readOnly = true)
    public ResumoEnergiaDto resumirMes(Long usuarioId, Long dispositivoId, int ano, int mes) {
        Dispositivo dispositivo = dispositivoDoUsuario(usuarioId, dispositivoId);
        if (ano < 2000 || ano > 2100 || mes < 1 || mes > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ano ou mês inválido");
        }
        YearMonth periodo = YearMonth.of(ano, mes);
        LocalDateTime inicio = periodo.atDay(1).atStartOfDay();
        LocalDateTime fim = periodo.plusMonths(1).atDay(1).atStartOfDay();

        List<LeituraEnergia> leiturasDoMes =
                leituras.findByDispositivoIdAndDataLeituraGreaterThanEqualAndDataLeituraLessThan(
                        dispositivoId, inicio, fim);
        BigDecimal totalKwh = leiturasDoMes.stream().map(LeituraEnergia::getKwh)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(6, RoundingMode.HALF_UP);
        BigDecimal custoTotal = leiturasDoMes.stream().map(LeituraEnergia::getCustoEstimado)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal meta = dispositivo.getMetaMensalReais();
        if (meta == null || meta.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Meta mensal inválida para o dispositivo");
        }
        BigDecimal percentual = custoTotal.multiply(BigDecimal.valueOf(100))
                .divide(meta, 2, RoundingMode.HALF_UP);
        FaixaEnergia faixa = custoTotal.compareTo(meta.multiply(new BigDecimal("0.70"))) < 0
                ? FaixaEnergia.VERDE
                : custoTotal.compareTo(meta) <= 0
                        ? FaixaEnergia.AMARELO : FaixaEnergia.VERMELHO;

        return new ResumoEnergiaDto(dispositivoId, ano, mes, totalKwh, custoTotal, meta,
                meta.subtract(custoTotal).setScale(2, RoundingMode.HALF_UP), percentual, faixa);
    }

    private Dispositivo dispositivoDoUsuario(Long usuarioId, Long dispositivoId) {
        Dispositivo dispositivo = dispositivos.findById(dispositivoId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispositivo não encontrado"));
        if (!usuarioId.equals(dispositivo.getUsuario().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispositivo não encontrado");
        }
        return dispositivo;
    }

    private void validarCodigoDisponivel(String codigo, Long dispositivoId) {
        dispositivos.findByCodigo(codigo)
                .filter(existente -> !existente.getId().equals(dispositivoId))
                .ifPresent(existente -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Código de dispositivo já cadastrado");
                });
    }

    private void preencherDispositivo(Dispositivo dispositivo, DispositivoForm form, String codigo) {
        dispositivo.setCodigo(codigo);
        dispositivo.setTarifaKwh(form.tarifaKwh());
        dispositivo.setMetaMensalReais(form.metaMensalReais());
    }
}
