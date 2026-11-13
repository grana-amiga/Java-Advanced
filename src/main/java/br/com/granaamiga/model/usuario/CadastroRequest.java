package br.com.granaamiga.model.usuario;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CadastroRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank String telefone,
        @NotBlank @Size(min = 8, max = 72) String senha,
        @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal rendaMensal
) {
}
