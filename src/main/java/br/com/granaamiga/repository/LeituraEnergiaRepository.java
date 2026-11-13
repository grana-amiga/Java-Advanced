package br.com.granaamiga.repository;

import br.com.granaamiga.model.energia.Dispositivo;
import br.com.granaamiga.model.energia.LeituraEnergia;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeituraEnergiaRepository extends JpaRepository<LeituraEnergia, Long> {

    List<LeituraEnergia> findByDispositivoId(Long dispositivoId);

    boolean existsByDispositivoId(Long dispositivoId);

    Page<LeituraEnergia> findByDispositivoIdOrderByDataLeituraDescIdDesc(
            Long dispositivoId, Pageable pageable);

    List<LeituraEnergia> findByDispositivoIdAndDataLeituraGreaterThanEqualAndDataLeituraLessThan(
            Long dispositivoId, LocalDateTime inicio, LocalDateTime fim);

    @Modifying
    @Query("delete from LeituraEnergia l where l.dispositivo.id in "
            + "(select d.id from Dispositivo d where d.usuario.id = :usuarioId)")
    int excluirDoUsuario(@Param("usuarioId") Long usuarioId);
}
