package br.com.citizentech.repository;

import br.com.citizentech.model.Divida;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DividaRepository extends JpaRepository<Divida, Long> {

    List<Divida> findByUsuarioId(Long usuarioId);

    @Modifying
    @Query("delete from Divida d where d.usuario.id = :usuarioId")
    int excluirDoUsuario(@Param("usuarioId") Long usuarioId);
}
