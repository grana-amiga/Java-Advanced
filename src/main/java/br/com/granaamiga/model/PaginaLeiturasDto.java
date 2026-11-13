package br.com.granaamiga.model;

import java.util.List;
import org.springframework.hateoas.EntityModel;

public record PaginaLeiturasDto(
        List<EntityModel<LeituraEnergiaDto>> itens,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas) {
}
