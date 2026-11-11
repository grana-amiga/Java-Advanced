package br.com.citizentech.divida;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/dividas")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dívidas", description = "Compromissos financeiros e projeção de quitação da conta autenticada")
public class DividaController {

    private final DividaService service;

    public DividaController(DividaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista as dívidas da conta autenticada")
    public List<EntityModel<DividaResponse>> listar(@AuthenticationPrincipal Jwt jwt) {
        return service.listar(usuarioId(jwt)).stream().map(this::links).toList();
    }

    @PostMapping
    @Operation(summary = "Cadastra uma dívida")
    public ResponseEntity<EntityModel<DividaResponse>> criar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DividaRequest request
    ) {
        DividaResponse criada = service.criar(usuarioId(jwt), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(location).body(links(criada));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma dívida da conta autenticada")
    public EntityModel<DividaResponse> buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return links(service.buscar(usuarioId(jwt), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma dívida")
    public EntityModel<DividaResponse> atualizar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody DividaRequest request
    ) {
        return links(service.atualizar(usuarioId(jwt), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui uma dívida")
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.excluir(usuarioId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/plano")
    @Operation(summary = "Projeta a quitação mensal da dívida")
    public EntityModel<PlanoQuitacaoResponse> plano(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        PlanoQuitacaoResponse resposta = service.plano(usuarioId(jwt), id);
        return EntityModel.of(resposta,
                Link.of("/api/v1/dividas/" + id + "/plano").withSelfRel(),
                Link.of("/api/v1/dividas/" + id).withRel("divida"));
    }

    private EntityModel<DividaResponse> links(DividaResponse dto) {
        String base = "/api/v1/dividas/" + dto.id();
        return EntityModel.of(dto,
                Link.of(base).withSelfRel(),
                Link.of(base + "/plano").withRel("plano"),
                Link.of("/api/v1/dividas").withRel("dividas"));
    }

    private Long usuarioId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
