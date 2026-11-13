package br.com.granaamiga.model.transacao;

import java.math.BigDecimal;

public record TotalCategoriaDto(Long categoriaId, String categoria, BigDecimal total) {
}
