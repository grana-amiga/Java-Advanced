package br.com.citizentech.model;

import br.com.citizentech.model.Papel;

public record UsuarioResumoResponse(
        Long id,
        String nome,
        String telefone,
        Papel papel
) {
}
