package br.com.granaamiga.controller;

import br.com.granaamiga.model.CategoriaDto;
import br.com.granaamiga.model.CategoriaForm;
import br.com.granaamiga.service.CategoriaService;
import br.com.granaamiga.model.TipoTransacao;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
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
public class CategoriaController {
    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @GetMapping
    public List<EntityModel<CategoriaDto>> listar(@RequestParam(required = false) TipoTransacao tipo) {
        return service.listar(tipo).stream().map(this::links).toList();
    }

    @GetMapping("/{id}")
    public EntityModel<CategoriaDto> buscar(@PathVariable Long id) {
        return links(CategoriaDto.from(service.buscarEntidade(id)));
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<EntityModel<CategoriaDto>> criar(@Valid @RequestBody CategoriaForm form) {
        CategoriaDto dto = service.criar(form);
        return ResponseEntity.created(URI.create("/api/v1/categorias/" + dto.id())).body(links(dto));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public EntityModel<CategoriaDto> atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaForm form) {
        return links(service.atualizar(id, form));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<CategoriaDto> links(CategoriaDto dto) {
        return EntityModel.of(dto,
                Link.of("/api/v1/categorias/" + dto.id()).withSelfRel(),
                Link.of("/api/v1/categorias").withRel("categorias"));
    }
}
