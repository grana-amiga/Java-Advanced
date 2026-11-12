package br.com.citizentech.service;

import br.com.citizentech.model.UsuarioPerfilResponse;
import br.com.citizentech.repository.DispositivoRepository;
import br.com.citizentech.repository.DividaRepository;
import br.com.citizentech.repository.LeituraEnergiaRepository;
import br.com.citizentech.repository.TransacaoRepository;
import br.com.citizentech.repository.UsuarioRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final LeituraEnergiaRepository leituras;
    private final DispositivoRepository dispositivos;
    private final TransacaoRepository transacoes;
    private final DividaRepository dividas;

    public UsuarioService(UsuarioRepository usuarios, LeituraEnergiaRepository leituras,
            DispositivoRepository dispositivos, TransacaoRepository transacoes, DividaRepository dividas) {
        this.usuarios = usuarios;
        this.leituras = leituras;
        this.dispositivos = dispositivos;
        this.transacoes = transacoes;
        this.dividas = dividas;
    }

    @Transactional(readOnly = true)
    public UsuarioPerfilResponse buscarMeuPerfil(Long usuarioId) {
        return usuarios.findById(usuarioId).map(UsuarioPerfilResponse::from).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conta não encontrada"));
    }

    @Transactional
    @CacheEvict(value = "resumoMensal", allEntries = true)
    public void excluirMinhaConta(Long usuarioId) {
        if (!usuarios.existsById(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conta não encontrada");
        }

        leituras.excluirDoUsuario(usuarioId);
        dispositivos.excluirDoUsuario(usuarioId);
        transacoes.excluirDoUsuario(usuarioId);
        dividas.excluirDoUsuario(usuarioId);

        if (usuarios.excluirPorId(usuarioId) != 1) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conta não encontrada");
        }
    }
}
