package br.com.citizentech.categoria;

import br.com.citizentech.domain.Categoria;
import br.com.citizentech.domain.TipoTransacao;
import br.com.citizentech.repository.CategoriaRepository;
import br.com.citizentech.repository.TransacaoRepository;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CategoriaService {
    private final CategoriaRepository repository;
    private final TransacaoRepository transacoes;

    public CategoriaService(CategoriaRepository repository, TransacaoRepository transacoes) {
        this.repository = repository;
        this.transacoes = transacoes;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "categorias", key = "#p0 == null ? 'todas' : #p0.name()")
    public List<CategoriaDto> listar(TipoTransacao tipo) {
        List<Categoria> categorias = tipo == null ? repository.findAll() : repository.findByTipo(tipo);
        return categorias.stream().map(CategoriaDto::from).toList();
    }

    @Transactional(readOnly = true)
    public Categoria buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada"));
    }

    @Transactional
    @CacheEvict(value = {"categorias", "resumoMensal"}, allEntries = true)
    public CategoriaDto criar(CategoriaForm form) {
        if (repository.findByNomeIgnoreCaseAndTipo(form.nome().trim(), form.tipo()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Categoria já existe");
        }
        Categoria categoria = new Categoria();
        preencher(categoria, form);
        return CategoriaDto.from(repository.save(categoria));
    }

    @Transactional
    @CacheEvict(value = {"categorias", "resumoMensal"}, allEntries = true)
    public CategoriaDto atualizar(Long id, CategoriaForm form) {
        Categoria categoria = buscarEntidade(id);
        if (categoria.getTipo() != form.tipo() && transacoes.existsByCategoriaId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível alterar o tipo de uma categoria já utilizada");
        }
        repository.findByNomeIgnoreCaseAndTipo(form.nome().trim(), form.tipo())
                .filter(outra -> !outra.getId().equals(id))
                .ifPresent(outra -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Categoria já existe"); });
        preencher(categoria, form);
        return CategoriaDto.from(repository.save(categoria));
    }

    @Transactional
    @CacheEvict(value = {"categorias", "resumoMensal"}, allEntries = true)
    public void excluir(Long id) {
        repository.delete(buscarEntidade(id));
    }

    private void preencher(Categoria categoria, CategoriaForm form) {
        categoria.setNome(form.nome().trim());
        categoria.setIcone(form.icone().trim());
        categoria.setTipo(form.tipo());
    }
}
