package br.com.citizentech.divida;

import java.math.BigDecimal;

public record ParcelaPlanoResponse(
        int mes,
        BigDecimal saldoInicial,
        BigDecimal juros,
        BigDecimal pagamento,
        BigDecimal amortizacao,
        BigDecimal saldoFinal
) {
}
