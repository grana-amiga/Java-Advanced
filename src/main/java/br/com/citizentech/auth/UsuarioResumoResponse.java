package br.com.citizentech.auth;

import br.com.citizentech.domain.Papel;

public record UsuarioResumoResponse(
        Long id,
        String nome,
        String telefone,
        Papel papel
) {
}
