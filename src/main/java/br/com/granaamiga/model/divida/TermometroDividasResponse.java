package br.com.granaamiga.model.divida;

import java.math.BigDecimal;

public record TermometroDividasResponse(
        BigDecimal rendaMensal,
        BigDecimal totalParcelasAbertas,
        BigDecimal percentualComprometimento,
        FaixaComprometimento faixa
) {
}
