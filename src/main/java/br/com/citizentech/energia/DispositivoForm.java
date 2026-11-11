package br.com.citizentech.energia;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record DispositivoForm(
        @NotBlank @Size(max = 80) String codigo,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 6, fraction = 6)
        BigDecimal tarifaKwh,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 17, fraction = 2)
        BigDecimal metaMensalReais) {
}
