package br.com.citizentech.model;

import java.math.BigDecimal;
import java.util.List;

public record PlanoQuitacaoResponse(
        Long dividaId,
        boolean quitavel,
        String aviso,
        Integer mesesParaQuitacao,
        int mesesSimulados,
        BigDecimal saldoInicial,
        BigDecimal taxaJurosMes,
        BigDecimal valorParcela,
        BigDecimal totalPagoSimulado,
        BigDecimal totalJurosSimulados,
        BigDecimal saldoRestante,
        List<ParcelaPlanoResponse> primeirasParcelas
) {
}
