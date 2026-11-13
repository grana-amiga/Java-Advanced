package br.com.granaamiga.model;

import br.com.granaamiga.model.Divida;
import br.com.granaamiga.model.StatusDivida;
import br.com.granaamiga.model.TipoDivida;
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
