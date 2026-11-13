package br.com.granaamiga.model;

import br.com.granaamiga.model.Papel;

public record UsuarioResumoResponse(
        Long id,
        String nome,
        String telefone,
        Papel papel
) {
}
