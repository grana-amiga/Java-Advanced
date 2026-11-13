package br.com.granaamiga.controller;

import br.com.granaamiga.model.usuario.UsuarioPerfilResponse;
import br.com.granaamiga.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Conta", description = "Perfil e exclusão da conta autenticada")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar meu perfil", description = "Retorna os dados da conta identificada pelo token.")
    public UsuarioPerfilResponse meuPerfil(@AuthenticationPrincipal Jwt jwt) {
        return service.buscarMeuPerfil(Long.valueOf(jwt.getSubject()));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Excluir minha conta", description = "Remove a conta autenticada e seus dados relacionados.")
    public ResponseEntity<Void> excluirMinhaConta(@AuthenticationPrincipal Jwt jwt) {
        service.excluirMinhaConta(Long.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }
}
