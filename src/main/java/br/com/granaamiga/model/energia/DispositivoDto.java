package br.com.granaamiga.model.energia;

import java.math.BigDecimal;

public record DispositivoDto(Long id, String codigo, BigDecimal tarifaKwh, BigDecimal metaMensalReais) {

    public static DispositivoDto from(Dispositivo dispositivo) {
        return new DispositivoDto(dispositivo.getId(), dispositivo.getCodigo(),
                dispositivo.getTarifaKwh(), dispositivo.getMetaMensalReais());
    }
}
