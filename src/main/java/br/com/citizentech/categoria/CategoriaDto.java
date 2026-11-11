package br.com.citizentech.categoria;

import br.com.citizentech.domain.Categoria;
import br.com.citizentech.domain.TipoTransacao;

public record CategoriaDto(Long id, String nome, String icone, TipoTransacao tipo) {
    public static CategoriaDto from(Categoria categoria) {
        return new CategoriaDto(categoria.getId(), categoria.getNome(), categoria.getIcone(),
                categoria.getTipo());
    }
}
