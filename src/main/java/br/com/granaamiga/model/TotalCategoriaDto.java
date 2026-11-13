package br.com.granaamiga.model;

import java.math.BigDecimal;

public record TotalCategoriaDto(Long categoriaId, String categoria, BigDecimal total) {
}
