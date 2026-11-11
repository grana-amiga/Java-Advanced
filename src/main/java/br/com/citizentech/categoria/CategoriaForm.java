package br.com.citizentech.categoria;

import br.com.citizentech.domain.TipoTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoriaForm(
        @NotBlank @Size(max = 80) String nome,
        @NotBlank @Size(max = 30) String icone,
        @NotNull TipoTransacao tipo) {
}
