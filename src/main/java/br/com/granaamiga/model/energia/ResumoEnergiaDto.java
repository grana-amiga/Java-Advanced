package br.com.granaamiga.model.energia;

import java.math.BigDecimal;

public record ResumoEnergiaDto(
        Long dispositivoId,
        int ano,
        int mes,
        BigDecimal totalKwh,
        BigDecimal custoTotal,
        BigDecimal metaMensalReais,
        BigDecimal diferencaParaMeta,
        BigDecimal percentualMeta,
        FaixaEnergia faixa) {
}
