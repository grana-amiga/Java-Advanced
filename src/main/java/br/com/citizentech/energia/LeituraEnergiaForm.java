package br.com.citizentech.energia;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LeituraEnergiaForm(
        LocalDateTime dataLeitura,
        @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 3) BigDecimal potenciaW,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 9, fraction = 6)
        BigDecimal kwh) {
}
