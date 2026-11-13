package br.com.granaamiga.controller;

import br.com.granaamiga.model.transacao.PaginaDto;
import br.com.granaamiga.model.transacao.ResumoMensalDto;
import br.com.granaamiga.model.transacao.TransacaoDto;
import br.com.granaamiga.model.transacao.TransacaoForm;
import br.com.granaamiga.model.transacao.OrigemTransacao;
import br.com.granaamiga.model.transacao.TipoTransacao;
import br.com.granaamiga.service.TransacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/transacoes")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Transações", description = "Entradas, saídas, extrato e resumo financeiro da conta")
public class TransacaoController {
    private final TransacaoService service;

    public TransacaoController(TransacaoService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Registrar transação", description = "Registra uma entrada ou saída da conta autenticada, com categoria do mesmo tipo.")
    public ResponseEntity<EntityModel<TransacaoDto>> criar(@AuthenticationPrincipal Jwt jwt,
                                                            @Valid @RequestBody TransacaoForm form) {
        TransacaoDto dto = service.criar(usuarioId(jwt), form, OrigemTransacao.APP);
        return ResponseEntity.created(URI.create("/api/v1/transacoes/" + dto.id())).body(links(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar transação", description = "Consulta uma transação pertencente à conta autenticada.")
    public EntityModel<TransacaoDto> buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return links(service.buscar(usuarioId(jwt), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar transação", description = "Altera uma transação pertencente à conta autenticada.")
    public EntityModel<TransacaoDto> atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                               @Valid @RequestBody TransacaoForm form) {
        return links(service.atualizar(usuarioId(jwt), id, form));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir transação", description = "Remove uma transação pertencente à conta autenticada.")
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.excluir(usuarioId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Listar transações", description = "Lista as transações da conta autenticada com filtros, paginação e ordenação.")
    public PaginaDto<EntityModel<TransacaoDto>> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) TipoTransacao tipo,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) BigDecimal valorMin,
            @RequestParam(required = false) BigDecimal valorMax,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho,
            @RequestParam(defaultValue = "dataTransacao") String ordenarPor,
            @RequestParam(defaultValue = "DESC") Sort.Direction direcao) {
        Page<TransacaoDto> resultado = service.listar(usuarioId(jwt), tipo, categoriaId, de, ate,
                valorMin, valorMax,
                pagina(pagina, tamanho, ordenarPor, direcao));
        return PaginaDto.from(resultado.map(this::links));
    }

    @GetMapping("/extrato")
    @Operation(summary = "Consultar extrato mensal", description = "Lista as transações da conta no mês informado, da mais recente para a mais antiga.")
    public PaginaDto<EntityModel<TransacaoDto>> extrato(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam int ano, @RequestParam int mes,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        Page<TransacaoDto> resultado = service.extratoMensal(usuarioId(jwt), ano, mes,
                pagina(pagina, tamanho, "dataTransacao", Sort.Direction.DESC));
        return PaginaDto.from(resultado.map(this::links));
    }

    @GetMapping("/resumo")
    @Operation(summary = "Consultar resumo mensal", description = "Soma entradas e saídas por categoria e calcula a diferença do mês; não consulta saldo bancário.")
    public ResumoMensalDto resumo(@AuthenticationPrincipal Jwt jwt,
                                  @RequestParam int ano, @RequestParam int mes) {
        return service.resumoMensal(usuarioId(jwt), ano, mes);
    }

    private Long usuarioId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    private PageRequest pagina(int numero, int tamanho, String campo, Sort.Direction direcao) {
        if (numero < 0 || tamanho < 1 || tamanho > 100 ||
                !(campo.equals("dataTransacao") || campo.equals("valor"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paginação ou ordenação inválida");
        }
        return PageRequest.of(numero, tamanho, Sort.by(direcao, campo).and(Sort.by(direcao, "id")));
    }

    private EntityModel<TransacaoDto> links(TransacaoDto dto) {
        Link transacao = Link.of("/api/v1/transacoes/" + dto.id());
        return EntityModel.of(dto,
                transacao.withSelfRel(),
                Link.of("/api/v1/categorias/" + dto.categoriaId()).withRel("categoria"),
                transacao.withRel("atualizar").withTitle("PUT"),
                transacao.withRel("excluir").withTitle("DELETE"));
    }
}
