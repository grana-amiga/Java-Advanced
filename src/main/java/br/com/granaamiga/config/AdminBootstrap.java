package br.com.granaamiga.config;

import br.com.granaamiga.model.usuario.Papel;
import br.com.granaamiga.model.usuario.Usuario;
import br.com.granaamiga.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrap {
    @Bean
    ApplicationRunner bootstrapAdmin(UsuarioRepository usuarios, PasswordEncoder encoder,
            @Value("${APP_BOOTSTRAP_ADMIN_PHONE:}") String telefoneConfigurado,
            @Value("${APP_BOOTSTRAP_ADMIN_PASSWORD:}") String senhaConfigurada) {
        return args -> {
            if (telefoneConfigurado.isBlank() && senhaConfigurada.isBlank()) return;
            String telefone = telefoneConfigurado.replaceAll("\\D", "");
            if (telefone.length() < 10 || telefone.length() > 13 || senhaConfigurada.length() < 8) {
                throw new IllegalStateException("Configure APP_BOOTSTRAP_ADMIN_PHONE e APP_BOOTSTRAP_ADMIN_PASSWORD (mínimo 8 caracteres)");
            }
            var existente = usuarios.findByTelefone(telefone);
            if (existente.isPresent()) {
                Usuario admin = existente.orElseThrow();
                if (admin.getPapel() != Papel.ADMIN) {
                    throw new IllegalStateException("O telefone do administrador já pertence a um usuário comum");
                }
                return;
            }
            Usuario admin = new Usuario();
            admin.setNome("Administrador");
            admin.setTelefone(telefone);
            admin.setSenhaHash(encoder.encode(senhaConfigurada));
            admin.setPapel(Papel.ADMIN);
            usuarios.save(admin);
        };
    }
}
