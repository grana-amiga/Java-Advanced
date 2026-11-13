package br.com.granaamiga.model;

import java.math.BigDecimal;

public record SimulacaoJurosResponse(
        BigDecimal montante,
        BigDecimal jurosTotais,
        String moeda) {
}
