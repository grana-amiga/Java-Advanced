package br.com.citizentech.model;

import java.math.BigDecimal;

public record SimulacaoJurosResponse(
        BigDecimal montante,
        BigDecimal jurosTotais,
        String moeda) {
}
