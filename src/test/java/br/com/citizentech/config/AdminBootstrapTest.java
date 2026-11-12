package br.com.citizentech.config;

import br.com.citizentech.model.Papel;
import br.com.citizentech.model.Usuario;
import br.com.citizentech.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void substituiHashDemonstrativoSomenteQuandoOperadorConfiguraSenha() throws Exception {
        Usuario admin = usuario(Papel.ADMIN, "HASH_DEMO_ADMINISTRADOR");
        when(usuarios.findByTelefone(admin.getTelefone())).thenReturn(Optional.of(admin));

        new AdminBootstrap().bootstrapAdmin(usuarios, encoder, admin.getTelefone(), "SenhaNova123!")
                .run(null);

        assertTrue(encoder.matches("SenhaNova123!", admin.getSenhaHash()));
        verify(usuarios).save(admin);
    }

    @Test
    void preservaSenhaRealDeAdministradorExistente() throws Exception {
        String hashAtual = encoder.encode("SenhaAtual123!");
        Usuario admin = usuario(Papel.ADMIN, hashAtual);
        when(usuarios.findByTelefone(admin.getTelefone())).thenReturn(Optional.of(admin));

        new AdminBootstrap().bootstrapAdmin(usuarios, encoder, admin.getTelefone(), "SenhaNova123!")
                .run(null);

        assertEquals(hashAtual, admin.getSenhaHash());
        verify(usuarios, never()).save(admin);
    }

    @Test
    void naoPromoveContaComumParaAdministrador() {
        Usuario comum = usuario(Papel.USER, "HASH_DEMO_USUARIO");
        when(usuarios.findByTelefone(comum.getTelefone())).thenReturn(Optional.of(comum));

        assertThrows(IllegalStateException.class,
                () -> new AdminBootstrap().bootstrapAdmin(usuarios, encoder,
                        comum.getTelefone(), "SenhaNova123!").run(null));
        verify(usuarios, never()).save(comum);
    }

    private Usuario usuario(Papel papel, String hash) {
        Usuario usuario = new Usuario();
        usuario.setTelefone("11900000010");
        usuario.setPapel(papel);
        usuario.setSenhaHash(hash);
        return usuario;
    }
}
