package br.com.granaamiga.model.usuario;

import java.time.Instant;

public record AuthResponse(
        String token,
        String tipo,
        Instant expiraEm,
        UsuarioResumoResponse usuario
) {
}
