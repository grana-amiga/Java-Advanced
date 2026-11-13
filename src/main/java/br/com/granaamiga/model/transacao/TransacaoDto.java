package br.com.granaamiga.model.transacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransacaoDto(Long id, Long categoriaId, String categoria, String icone,
                           BigDecimal valor, TipoTransacao tipo, String descricao,
                           LocalDateTime dataTransacao, OrigemTransacao origem) {
    public static TransacaoDto from(Transacao transacao) {
        return new TransacaoDto(transacao.getId(), transacao.getCategoria().getId(),
                transacao.getCategoria().getNome(), transacao.getCategoria().getIcone(),
                transacao.getValor(), transacao.getTipo(), transacao.getDescricao(),
                transacao.getDataTransacao(), transacao.getOrigem());
    }
}
