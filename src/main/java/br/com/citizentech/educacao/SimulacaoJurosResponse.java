package br.com.citizentech.educacao;

import java.math.BigDecimal;

public record SimulacaoJurosResponse(
        BigDecimal montante,
        BigDecimal jurosTotais,
        String moeda) {
}
