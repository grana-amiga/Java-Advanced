package br.com.granaamiga.controller;

import br.com.granaamiga.model.AuthResponse;
import br.com.granaamiga.model.CadastroRequest;
import br.com.granaamiga.model.LoginRequest;
import br.com.granaamiga.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<AuthResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authenticationService.cadastrar(request));
    }

    @PostMapping("/login")
    public AuthResponse entrar(@Valid @RequestBody LoginRequest request) {
        return authenticationService.entrar(request);
    }
}
