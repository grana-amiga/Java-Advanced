package br.com.granaamiga.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SimulacaoJurosRequest(
        @NotNull @DecimalMin("0") @DecimalMax("1000000000.00") BigDecimal valorInicial,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal taxaMensal,
        @Min(1) @Max(600) int meses) {
}
