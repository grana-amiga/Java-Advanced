package br.com.citizentech.repository;

import br.com.citizentech.domain.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByTelefone(String telefone);

    boolean existsByTelefone(String telefone);

    @Modifying
    @Query("delete from Usuario u where u.id = :usuarioId")
    int excluirPorId(@Param("usuarioId") Long usuarioId);
}
