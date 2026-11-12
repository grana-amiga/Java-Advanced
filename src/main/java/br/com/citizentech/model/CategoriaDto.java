package br.com.citizentech.model;

import br.com.citizentech.model.Categoria;
import br.com.citizentech.model.TipoTransacao;

public record CategoriaDto(Long id, String nome, String icone, TipoTransacao tipo) {
    public static CategoriaDto from(Categoria categoria) {
        return new CategoriaDto(categoria.getId(), categoria.getNome(), categoria.getIcone(),
                categoria.getTipo());
    }
}
