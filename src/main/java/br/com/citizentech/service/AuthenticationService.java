package br.com.citizentech.service;

import br.com.citizentech.model.AuthResponse;
import br.com.citizentech.model.CadastroRequest;
import br.com.citizentech.model.LoginRequest;
import br.com.citizentech.model.UsuarioResumoResponse;
import br.com.citizentech.model.Papel;
import br.com.citizentech.model.Usuario;
import br.com.citizentech.repository.UsuarioRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthenticationService {

    private static final String EMISSOR = "citizentech";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    public AuthenticationService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
    }

    @Transactional
    public AuthResponse cadastrar(CadastroRequest request) {
        String telefone = normalizarTelefone(request.telefone());
        if (usuarios.existsByTelefone(telefone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Telefone já cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome().trim());
        usuario.setTelefone(telefone);
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setPapel(Papel.USER);
        usuario.setRendaMensal(request.rendaMensal());
        try {
            // O flush também captura colisões entre dois cadastros simultâneos.
            usuario = usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dados de cadastro em conflito", ex);
        }
        return criarResposta(usuario);
    }

    @Transactional(readOnly = true)
    public AuthResponse entrar(LoginRequest request) {
        String telefone = normalizarTelefone(request.telefone());
        Usuario usuario = usuarios.findByTelefone(telefone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Telefone ou senha inválidos"));
        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Telefone ou senha inválidos");
        }
        return criarResposta(usuario);
    }

    private AuthResponse criarResposta(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(2, ChronoUnit.HOURS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .subject(usuario.getId().toString())
                .claim("roles", List.of(usuario.getPapel().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthResponse(
                token,
                "Bearer",
                expiraEm,
                new UsuarioResumoResponse(usuario.getId(), usuario.getNome(), usuario.getTelefone(), usuario.getPapel())
        );
    }

    private String normalizarTelefone(String telefone) {
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.length() < 10 || digitos.length() > 13) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Telefone deve conter de 10 a 13 dígitos");
        }
        return digitos;
    }
}
