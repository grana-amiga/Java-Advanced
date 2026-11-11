package br.com.citizentech.transacao;

import java.math.BigDecimal;

public record TotalCategoriaDto(Long categoriaId, String categoria, BigDecimal total) {
}
