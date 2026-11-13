package br.com.granaamiga.service;

import br.com.granaamiga.model.categoria.Categoria;
import br.com.granaamiga.model.transacao.OrigemTransacao;
import br.com.granaamiga.model.transacao.ResumoMensalDto;
import br.com.granaamiga.model.transacao.TipoTransacao;
import br.com.granaamiga.model.transacao.TotalCategoriaDto;
import br.com.granaamiga.model.transacao.Transacao;
import br.com.granaamiga.model.transacao.TransacaoDto;
import br.com.granaamiga.model.transacao.TransacaoForm;
import br.com.granaamiga.repository.TransacaoRepository;
import br.com.granaamiga.repository.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransacaoService {
    private final TransacaoRepository repository;
    private final UsuarioRepository usuarios;
    private final CategoriaService categorias;
    private final Clock clock;

    public TransacaoService(TransacaoRepository repository, UsuarioRepository usuarios,
                            CategoriaService categorias, Clock clock) {
        this.repository = repository;
        this.usuarios = usuarios;
        this.categorias = categorias;
        this.clock = clock;
    }

    @Transactional
    @CacheEvict(value = "resumoMensal", allEntries = true)
    public TransacaoDto criar(Long usuarioId, TransacaoForm form, OrigemTransacao origem) {
        Transacao transacao = new Transacao();
        transacao.setUsuario(usuarios.getReferenceById(usuarioId));
        preencher(transacao, form, origem);
        return TransacaoDto.from(repository.saveAndFlush(transacao));
    }

    @Transactional(readOnly = true)
    public TransacaoDto buscar(Long usuarioId, Long id) {
        return TransacaoDto.from(buscarDoUsuario(usuarioId, id));
    }

    @Transactional
    @CacheEvict(value = "resumoMensal", allEntries = true)
    public TransacaoDto atualizar(Long usuarioId, Long id, TransacaoForm form) {
        Transacao transacao = buscarDoUsuario(usuarioId, id);
        preencher(transacao, form, transacao.getOrigem());
        return TransacaoDto.from(repository.saveAndFlush(transacao));
    }

    @Transactional
    @CacheEvict(value = "resumoMensal", allEntries = true)
    public void excluir(Long usuarioId, Long id) {
        repository.delete(buscarDoUsuario(usuarioId, id));
    }

    @Transactional(readOnly = true)
    public Page<TransacaoDto> listar(Long usuarioId, TipoTransacao tipo, Long categoriaId,
                                     LocalDate de, LocalDate ate, BigDecimal valorMin,
                                     BigDecimal valorMax, Pageable pageable) {
        if (de != null && (de.getYear() < 2000 || de.getYear() > 2100)
                || ate != null && (ate.getYear() < 2000 || ate.getYear() > 2100)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datas devem estar entre 2000 e 2100");
        }
        if (de != null && ate != null && de.isAfter(ate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data inicial após data final");
        }
        if (valorMin != null && valorMin.signum() < 0 || valorMax != null && valorMax.signum() < 0
                || valorMin != null && valorMax != null && valorMin.compareTo(valorMax) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faixa de valor inválida");
        }
        Specification<Transacao> filtro = (root, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            condicoes.add(cb.equal(root.get("usuario").get("id"), usuarioId));
            if (tipo != null) condicoes.add(cb.equal(root.get("tipo"), tipo));
            if (categoriaId != null) condicoes.add(cb.equal(root.get("categoria").get("id"), categoriaId));
            if (de != null) condicoes.add(cb.greaterThanOrEqualTo(root.get("dataTransacao"), de.atStartOfDay()));
            if (ate != null) condicoes.add(cb.lessThan(root.get("dataTransacao"), ate.plusDays(1).atStartOfDay()));
            if (valorMin != null) condicoes.add(cb.greaterThanOrEqualTo(root.get("valor"), valorMin));
            if (valorMax != null) condicoes.add(cb.lessThanOrEqualTo(root.get("valor"), valorMax));
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
        return repository.findAll(filtro, pageable).map(TransacaoDto::from);
    }

    @Transactional(readOnly = true)
    public Page<TransacaoDto> extratoMensal(Long usuarioId, int ano, int mes, Pageable pageable) {
        YearMonth periodo = periodo(ano, mes);
        LocalDateTime inicio = periodo.atDay(1).atStartOfDay();
        LocalDateTime fim = periodo.plusMonths(1).atDay(1).atStartOfDay();
        return repository.findByUsuarioIdAndPeriodo(usuarioId, inicio, fim, pageable)
                .map(TransacaoDto::from);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "resumoMensal", key = "#p0 + ':' + #p1 + ':' + #p2")
    public ResumoMensalDto resumoMensal(Long usuarioId, int ano, int mes) {
        YearMonth periodo = periodo(ano, mes);
        LocalDateTime inicio = periodo.atDay(1).atStartOfDay();
        LocalDateTime fim = periodo.plusMonths(1).atDay(1).atStartOfDay();
        List<TotalCategoriaDto> entradas = totais(usuarioId, TipoTransacao.ENTRADA, inicio, fim);
        List<TotalCategoriaDto> saidas = totais(usuarioId, TipoTransacao.SAIDA, inicio, fim);
        BigDecimal totalEntradas = somar(entradas);
        BigDecimal totalSaidas = somar(saidas);
        return new ResumoMensalDto(ano, mes, totalEntradas, totalSaidas,
                totalEntradas.subtract(totalSaidas), entradas, saidas);
    }

    private List<TotalCategoriaDto> totais(Long usuarioId, TipoTransacao tipo,
                                            LocalDateTime inicio, LocalDateTime fim) {
        return repository.somarPorCategoria(usuarioId, tipo, inicio, fim).stream()
                .map(linha -> new TotalCategoriaDto((Long) linha[0], (String) linha[1], (BigDecimal) linha[2]))
                .toList();
    }

    private BigDecimal somar(List<TotalCategoriaDto> linhas) {
        return linhas.stream().map(TotalCategoriaDto::total)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    private Transacao buscarDoUsuario(Long usuarioId, Long id) {
        Transacao transacao = repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Transação não encontrada"));
        if (!transacao.getUsuario().getId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transação não encontrada");
        }
        return transacao;
    }

    private void preencher(Transacao transacao, TransacaoForm form, OrigemTransacao origem) {
        Categoria categoria = categorias.buscarEntidade(form.categoriaId());
        if (categoria.getTipo() != form.tipo()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo da categoria incompatível com a transação");
        }
        transacao.setCategoria(categoria);
        transacao.setValor(form.valor());
        transacao.setTipo(form.tipo());
        transacao.setDescricao(form.descricao() == null ? null : form.descricao().trim());
        if (form.dataTransacao() != null) {
            transacao.setDataTransacao(form.dataTransacao());
        } else if (transacao.getDataTransacao() == null) {
            transacao.setDataTransacao(LocalDateTime.now(clock));
        }
        transacao.setOrigem(origem);
    }

    private YearMonth periodo(int ano, int mes) {
        if (ano < 2000 || ano > 2100 || mes < 1 || mes > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ano ou mês inválido");
        }
        return YearMonth.of(ano, mes);
    }
}
