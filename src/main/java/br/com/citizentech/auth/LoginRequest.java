package br.com.citizentech.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String telefone,
        @NotBlank String senha
) {
}
