package br.com.granaamiga.model.transacao;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransacaoForm(
        @NotNull Long categoriaId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal valor,
        @NotNull TipoTransacao tipo,
        @Size(max = 500) String descricao,
        LocalDateTime dataTransacao) {
}
