package br.com.granaamiga.repository;

import br.com.granaamiga.model.Categoria;
import br.com.granaamiga.model.TipoTransacao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByTipo(TipoTransacao tipo);

    Optional<Categoria> findByNomeIgnoreCaseAndTipo(String nome, TipoTransacao tipo);
}
