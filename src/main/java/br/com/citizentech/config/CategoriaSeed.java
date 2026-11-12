package br.com.citizentech.config;

import br.com.citizentech.model.Categoria;
import br.com.citizentech.model.TipoTransacao;
import br.com.citizentech.repository.CategoriaRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CategoriaSeed {
    @Bean
    ApplicationRunner categoriasIniciais(CategoriaRepository repository) {
        return args -> {
            if (repository.count() > 0) return;
            salvar(repository, "Salário", "💰", TipoTransacao.ENTRADA);
            salvar(repository, "Outras entradas", "➕", TipoTransacao.ENTRADA);
            salvar(repository, "Alimentação", "🥖", TipoTransacao.SAIDA);
            salvar(repository, "Mercado", "🛒", TipoTransacao.SAIDA);
            salvar(repository, "Transporte", "🚌", TipoTransacao.SAIDA);
            salvar(repository, "Moradia", "🏠", TipoTransacao.SAIDA);
            salvar(repository, "Energia", "🔌", TipoTransacao.SAIDA);
            salvar(repository, "Saúde", "💊", TipoTransacao.SAIDA);
            salvar(repository, "Lazer", "🎮", TipoTransacao.SAIDA);
            salvar(repository, "Outros gastos", "📦", TipoTransacao.SAIDA);
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
