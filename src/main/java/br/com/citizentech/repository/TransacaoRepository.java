package br.com.citizentech.repository;

import br.com.citizentech.model.TipoTransacao;
import br.com.citizentech.model.Transacao;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransacaoRepository extends JpaRepository<Transacao, Long>, JpaSpecificationExecutor<Transacao> {

    boolean existsByCategoriaId(Long categoriaId);

    @Query("""
            select t from Transacao t
            where t.usuario.id = :usuarioId
              and t.dataTransacao >= :inicio
              and t.dataTransacao < :fim
            """)
    Page<Transacao> findByUsuarioIdAndPeriodo(
            @Param("usuarioId") Long usuarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            Pageable pageable);

    @Query("""
            select c.id, c.nome, sum(t.valor)
            from Transacao t join t.categoria c
            where t.usuario.id = :usuarioId
              and t.tipo = :tipo
              and t.dataTransacao >= :inicio
              and t.dataTransacao < :fim
            group by c.id, c.nome
            order by c.nome
            """)
    List<Object[]> somarPorCategoria(
            @Param("usuarioId") Long usuarioId,
            @Param("tipo") TipoTransacao tipo,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Modifying
    @Query("delete from Transacao t where t.usuario.id = :usuarioId")
    int excluirDoUsuario(@Param("usuarioId") Long usuarioId);
}
