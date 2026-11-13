package br.com.granaamiga.model.categoria;

import br.com.granaamiga.model.transacao.TipoTransacao;

public record CategoriaDto(Long id, String nome, String icone, TipoTransacao tipo) {
    public static CategoriaDto from(Categoria categoria) {
        return new CategoriaDto(categoria.getId(), categoria.getNome(), categoria.getIcone(),
                categoria.getTipo());
    }
}
