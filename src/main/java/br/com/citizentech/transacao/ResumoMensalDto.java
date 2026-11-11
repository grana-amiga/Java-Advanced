package br.com.citizentech.transacao;

import java.math.BigDecimal;
import java.util.List;

public record ResumoMensalDto(int ano, int mes, BigDecimal entradas, BigDecimal saidas,
                              BigDecimal saldo, List<TotalCategoriaDto> entradasPorCategoria,
                              List<TotalCategoriaDto> saidasPorCategoria) {
}
