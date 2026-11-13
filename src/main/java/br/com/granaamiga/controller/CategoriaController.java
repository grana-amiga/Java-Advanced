package br.com.granaamiga.controller;

import br.com.granaamiga.model.categoria.CategoriaDto;
import br.com.granaamiga.model.categoria.CategoriaForm;
import br.com.granaamiga.model.transacao.TipoTransacao;
import br.com.granaamiga.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorias", description = "Catálogo público de categorias de entrada e saída")
public class CategoriaController {
    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar categorias", description = "Consulta categorias sem login, com filtro opcional por tipo.")
    public List<EntityModel<CategoriaDto>> listar(@RequestParam(required = false) TipoTransacao tipo) {
        return service.listar(tipo).stream().map(this::links).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar categoria", description = "Busca uma categoria pelo identificador sem exigir login.")
    public EntityModel<CategoriaDto> buscar(@PathVariable Long id) {
        return links(CategoriaDto.from(service.buscarEntidade(id)));
    }

    @PostMapping
    @Operation(summary = "Criar categoria", description = "Adiciona uma categoria ao catálogo; exige papel ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<EntityModel<CategoriaDto>> criar(@Valid @RequestBody CategoriaForm form) {
        CategoriaDto dto = service.criar(form);
        return ResponseEntity.created(URI.create("/api/v1/categorias/" + dto.id())).body(links(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar categoria", description = "Altera nome e ícone de uma categoria; exige papel ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public EntityModel<CategoriaDto> atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaForm form) {
        return links(service.atualizar(id, form));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir categoria", description = "Remove uma categoria sem transações vinculadas; exige papel ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<CategoriaDto> links(CategoriaDto dto) {
        Link categoria = Link.of("/api/v1/categorias/" + dto.id());
        EntityModel<CategoriaDto> recurso = EntityModel.of(dto,
                categoria.withSelfRel(),
                Link.of("/api/v1/categorias").withRel("categorias"));
        if (administradorAutenticado()) {
            recurso.add(categoria.withRel("atualizar").withTitle("PUT"),
                    categoria.withRel("excluir").withTitle("DELETE"));
        }
        return recurso;
    }

    private boolean administradorAutenticado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao != null && autenticacao.getAuthorities().stream()
                .anyMatch(permissao -> "ROLE_ADMIN".equals(permissao.getAuthority()));
    }
}
