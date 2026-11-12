package br.com.citizentech.model;

import java.math.BigDecimal;

public record SimulacaoPoupancaResponse(
        BigDecimal totalAportado,
        BigDecimal saldoFinal,
        BigDecimal rendimento,
        String moeda) {
}
