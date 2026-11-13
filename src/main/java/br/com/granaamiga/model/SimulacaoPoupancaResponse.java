package br.com.granaamiga.model;

import java.math.BigDecimal;

public record SimulacaoPoupancaResponse(
        BigDecimal totalAportado,
        BigDecimal saldoFinal,
        BigDecimal rendimento,
        String moeda) {
}
