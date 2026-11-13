package br.com.granaamiga.repository;

import br.com.granaamiga.model.energia.Dispositivo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {

    List<Dispositivo> findByUsuarioId(Long usuarioId);

    Optional<Dispositivo> findByCodigo(String codigo);

    @Modifying
    @Query("delete from Dispositivo d where d.usuario.id = :usuarioId")
    int excluirDoUsuario(@Param("usuarioId") Long usuarioId);
}
