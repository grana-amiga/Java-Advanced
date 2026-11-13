package br.com.granaamiga.model.categoria;

import br.com.granaamiga.model.transacao.TipoTransacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoriaForm(
        @NotBlank @Size(max = 80) String nome,
        @NotBlank @Size(max = 100) String icone,
        @NotNull TipoTransacao tipo) {
}
