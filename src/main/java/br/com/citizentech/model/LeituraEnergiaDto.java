package br.com.citizentech.model;

import br.com.citizentech.model.LeituraEnergia;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LeituraEnergiaDto(
        Long id,
        Long dispositivoId,
        LocalDateTime dataLeitura,
        BigDecimal potenciaW,
        BigDecimal kwh,
        BigDecimal custoEstimado) {

    public static LeituraEnergiaDto from(LeituraEnergia leitura) {
        return new LeituraEnergiaDto(leitura.getId(), leitura.getDispositivo().getId(),
                leitura.getDataLeitura(), leitura.getPotenciaW(), leitura.getKwh(),
                leitura.getCustoEstimado());
    }
}
