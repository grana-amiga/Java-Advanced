package br.com.granaamiga.model;

import br.com.granaamiga.model.StatusDivida;
import br.com.granaamiga.model.TipoDivida;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record DividaRequest(
        @NotBlank @Size(max = 120) String credor,
        @NotNull TipoDivida tipo,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal saldoDevedor,
        @NotNull @DecimalMin("0.00") @Digits(integer = 6, fraction = 4) BigDecimal taxaJurosMes,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal valorParcela,
        @NotNull @Min(1) @Max(31) Integer diaVencimento,
        StatusDivida status
) {
}
