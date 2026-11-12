package br.com.citizentech.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UsuarioPerfilResponse(
        Long id,
        String nome,
        String telefone,
        Papel papel,
        BigDecimal rendaMensal,
        LocalDateTime dataCadastro
) {
    public static UsuarioPerfilResponse from(Usuario usuario) {
        return new UsuarioPerfilResponse(usuario.getId(), usuario.getNome(), usuario.getTelefone(),
                usuario.getPapel(), usuario.getRendaMensal(), usuario.getDataCadastro());
    }
}
