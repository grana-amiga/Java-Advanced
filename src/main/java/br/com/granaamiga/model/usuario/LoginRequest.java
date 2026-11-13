package br.com.granaamiga.model.usuario;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String telefone,
        @NotBlank String senha
) {
}
