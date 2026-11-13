package br.com.granaamiga.model;

import java.time.Instant;

public record AuthResponse(
        String token,
        String tipo,
        Instant expiraEm,
        UsuarioResumoResponse usuario
) {
}
