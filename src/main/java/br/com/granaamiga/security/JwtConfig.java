package br.com.granaamiga.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

    private static final String EMISSOR = "grana-amiga";

    @Bean
    public SecretKey jwtSecretKey(@Value("${APP_JWT_SECRET:}") String configurado) {
        byte[] segredo;
        if (configurado == null || configurado.isBlank()) {
            // Modo de demonstração: tokens anteriores deixam de valer a cada reinício.
            segredo = new byte[32];
            new SecureRandom().nextBytes(segredo);
        } else {
            segredo = configurado.getBytes(StandardCharsets.UTF_8);
            if (segredo.length < 32) {
                throw new IllegalStateException("APP_JWT_SECRET deve ter pelo menos 32 bytes em UTF-8");
            }
        }
        return new SecretKeySpec(segredo, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISSOR));
        return decoder;
    }
}
