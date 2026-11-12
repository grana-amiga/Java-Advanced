package br.com.citizentech.model;

import java.math.BigDecimal;

public record TermometroDividasResponse(
        BigDecimal rendaMensal,
        BigDecimal totalParcelasAbertas,
        BigDecimal percentualComprometimento,
        FaixaComprometimento faixa
) {
}
