package br.com.citizentech.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AlertaVencimentoResponse(
        Long dividaId,
        String credor,
        BigDecimal valorParcela,
        LocalDate dataVencimento,
        long diasRestantes,
        boolean venceNestaSemana
) {
}
