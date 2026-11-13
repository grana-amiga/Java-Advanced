package br.com.granaamiga.controller;

import br.com.granaamiga.model.DispositivoDto;
import br.com.granaamiga.model.DispositivoForm;
import br.com.granaamiga.model.LeituraEnergiaDto;
import br.com.granaamiga.model.LeituraEnergiaForm;
import br.com.granaamiga.model.PaginaLeiturasDto;
import br.com.granaamiga.model.ResumoEnergiaDto;
import br.com.granaamiga.service.EnergiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/dispositivos")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Energia", description = "Dispositivos e leituras de consumo da família autenticada")
public class EnergiaController {

    private final EnergiaService service;

    public EnergiaController(EnergiaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista os dispositivos da família autenticada")
    public List<EntityModel<DispositivoDto>> listar(@AuthenticationPrincipal Jwt jwt) {
        return service.listarDispositivos(usuarioId(jwt)).stream().map(this::linksDispositivo).toList();
    }

    @PostMapping
    @Operation(summary = "Cadastra um dispositivo de energia")
    public ResponseEntity<EntityModel<DispositivoDto>> criar(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DispositivoForm form) {
        DispositivoDto dto = service.criarDispositivo(usuarioId(jwt), form);
        return ResponseEntity.created(URI.create("/api/v1/dispositivos/" + dto.id()))
                .body(linksDispositivo(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um dispositivo da família autenticada")
    public EntityModel<DispositivoDto> buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return linksDispositivo(service.buscarDispositivo(usuarioId(jwt), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza tarifa, meta mensal e código do dispositivo")
    public EntityModel<DispositivoDto> atualizar(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id, @Valid @RequestBody DispositivoForm form) {
        return linksDispositivo(service.atualizarDispositivo(usuarioId(jwt), id, form));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui dispositivo sem leituras registradas")
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.excluirDispositivo(usuarioId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/leituras")
    @Operation(summary = "Lista as leituras da família com paginação e ordem decrescente por data e ID")
    public PaginaLeiturasDto listarLeituras(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        Page<LeituraEnergiaDto> resultado = service.listarLeituras(usuarioId(jwt), id, pagina, tamanho);
        return new PaginaLeiturasDto(resultado.getContent().stream().map(this::linksLeitura).toList(),
                resultado.getNumber(), resultado.getSize(), resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    @PostMapping("/{id}/leituras")
    @Operation(summary = "Registra leitura e calcula o custo pela tarifa do dispositivo")
    public ResponseEntity<EntityModel<LeituraEnergiaDto>> registrarLeitura(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody LeituraEnergiaForm form) {
        LeituraEnergiaDto dto = service.registrarLeitura(usuarioId(jwt), id, form);
        return ResponseEntity.created(URI.create("/api/v1/dispositivos/" + id + "/leituras/" + dto.id()))
                .body(linksLeitura(dto));
    }

    @GetMapping("/{id}/leituras/{leituraId}")
    @Operation(summary = "Consulta uma leitura do dispositivo da família")
    public EntityModel<LeituraEnergiaDto> buscarLeitura(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id, @PathVariable Long leituraId) {
        return linksLeitura(service.buscarLeitura(usuarioId(jwt), id, leituraId));
    }

    @GetMapping("/{id}/resumo")
    @Operation(summary = "Resume consumo, custo e progresso da meta no mês")
    public EntityModel<ResumoEnergiaDto> resumo(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id, @RequestParam int ano, @RequestParam int mes) {
        ResumoEnergiaDto dto = service.resumirMes(usuarioId(jwt), id, ano, mes);
        return EntityModel.of(dto,
                Link.of("/api/v1/dispositivos/" + id + "/resumo?ano=" + ano + "&mes=" + mes)
                        .withSelfRel(),
                Link.of("/api/v1/dispositivos/" + id).withRel("dispositivo"));
    }

    private EntityModel<DispositivoDto> linksDispositivo(DispositivoDto dto) {
        String base = "/api/v1/dispositivos/" + dto.id();
        return EntityModel.of(dto,
                Link.of(base).withSelfRel(),
                Link.of(base + "/leituras").withRel("leituras"),
                Link.of(base + "/resumo{?ano,mes}").withRel("resumo"),
                Link.of("/api/v1/dispositivos").withRel("dispositivos"));
    }

    private EntityModel<LeituraEnergiaDto> linksLeitura(LeituraEnergiaDto dto) {
        String base = "/api/v1/dispositivos/" + dto.dispositivoId();
        return EntityModel.of(dto,
                Link.of(base + "/leituras/" + dto.id()).withSelfRel(),
                Link.of(base).withRel("dispositivo"),
                Link.of(base + "/leituras").withRel("leituras"));
    }

    private Long usuarioId(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária");
        }
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
    }
}
