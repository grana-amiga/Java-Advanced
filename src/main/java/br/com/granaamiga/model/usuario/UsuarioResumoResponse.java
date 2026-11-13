package br.com.granaamiga.model.usuario;


public record UsuarioResumoResponse(
        Long id,
        String nome,
        String telefone,
        Papel papel
) {
}
