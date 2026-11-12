package br.com.citizentech.model;

import java.math.BigDecimal;

public record TotalCategoriaDto(Long categoriaId, String categoria, BigDecimal total) {
}
