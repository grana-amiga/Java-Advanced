package br.com.citizentech.model;

import br.com.citizentech.model.Divida;
import br.com.citizentech.model.StatusDivida;
import br.com.citizentech.model.TipoDivida;
import java.math.BigDecimal;

public record DividaResponse(
        Long id,
        String credor,
        TipoDivida tipo,
        BigDecimal saldoDevedor,
        BigDecimal taxaJurosMes,
        BigDecimal valorParcela,
        Integer diaVencimento,
        StatusDivida status
) {
    public static DividaResponse from(Divida divida) {
        return new DividaResponse(
                divida.getId(),
                divida.getCredor(),
                divida.getTipo(),
                divida.getSaldoDevedor(),
                divida.getTaxaJurosMes(),
                divida.getValorParcela(),
                divida.getDiaVencimento(),
                divida.getStatus()
        );
    }
}
