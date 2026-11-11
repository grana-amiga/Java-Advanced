package br.com.citizentech.transacao;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaDto<T>(List<T> itens, int pagina, int tamanho,
                           long totalElementos, int totalPaginas) {
    public static <T> PaginaDto<T> from(Page<T> page) {
        return new PaginaDto<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
