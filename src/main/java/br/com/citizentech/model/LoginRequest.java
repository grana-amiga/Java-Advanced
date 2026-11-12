package br.com.citizentech.model;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String telefone,
        @NotBlank String senha
) {
}
