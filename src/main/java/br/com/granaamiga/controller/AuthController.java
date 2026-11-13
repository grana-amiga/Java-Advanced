package br.com.granaamiga.controller;

import br.com.granaamiga.model.usuario.AuthResponse;
import br.com.granaamiga.model.usuario.CadastroRequest;
import br.com.granaamiga.model.usuario.LoginRequest;
import br.com.granaamiga.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Cadastro e login por telefone e senha")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/cadastro")
    @Operation(summary = "Criar conta", description = "Cadastra um usuário comum e devolve um token de acesso.")
    public ResponseEntity<AuthResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authenticationService.cadastrar(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Entrar na conta", description = "Valida telefone e senha e devolve um token de acesso.")
    public AuthResponse entrar(@Valid @RequestBody LoginRequest request) {
        return authenticationService.entrar(request);
    }
}
