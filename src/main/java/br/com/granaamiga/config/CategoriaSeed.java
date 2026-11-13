package br.com.granaamiga.config;

import br.com.granaamiga.model.Categoria;
import br.com.granaamiga.model.TipoTransacao;
import br.com.granaamiga.repository.CategoriaRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CategoriaSeed {
    @Bean
    ApplicationRunner categoriasIniciais(CategoriaRepository repository) {
        return args -> {
            if (repository.count() > 0) return;
            salvar(repository, "Salário", "attach_money", TipoTransacao.ENTRADA);
            salvar(repository, "Outras entradas", "add_circle_outline", TipoTransacao.ENTRADA);
            salvar(repository, "Alimentação", "restaurant", TipoTransacao.SAIDA);
            salvar(repository, "Mercado", "shopping_cart", TipoTransacao.SAIDA);
            salvar(repository, "Transporte", "directions_bus", TipoTransacao.SAIDA);
            salvar(repository, "Moradia", "home", TipoTransacao.SAIDA);
            salvar(repository, "Energia", "bolt", TipoTransacao.SAIDA);
            salvar(repository, "Saúde", "medication", TipoTransacao.SAIDA);
            salvar(repository, "Lazer", "sports_esports", TipoTransacao.SAIDA);
            salvar(repository, "Outros gastos", "inventory_2", TipoTransacao.SAIDA);
        };
    }

    private void salvar(CategoriaRepository repository, String nome, String icone, TipoTransacao tipo) {
        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        categoria.setIcone(icone);
        categoria.setTipo(tipo);
        repository.save(categoria);
    }
}
