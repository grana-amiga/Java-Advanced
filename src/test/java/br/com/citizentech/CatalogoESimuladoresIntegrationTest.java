package br.com.citizentech;

import br.com.citizentech.model.Papel;
import br.com.citizentech.model.Usuario;
import br.com.citizentech.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalogo_simuladores_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "APP_JWT_SECRET=segredo-de-teste-catalogo-com-mais-de-trinta-e-dois-bytes"
})
class CatalogoESimuladoresIntegrationTest {

    private static final String SENHA = "senha-de-teste-123";

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;

    @Test
    void adminCriaConsultaAtualizaEExcluiCategoria() throws Exception {
        String token = tokenAdmin();
        String nome = "Categoria " + UUID.randomUUID();
        MvcResult criacao = mvc.perform(post("/api/v1/categorias")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoriaJson(nome, "X", "SAIDA")))
                .andExpect(status().isCreated()).andReturn();
        long id = ((Number) JsonPath.read(criacao.getResponse().getContentAsString(), "$.id")).longValue();
        assertEquals("/api/v1/categorias/" + id, criacao.getResponse().getHeader("Location"));

        MvcResult busca = mvc.perform(get("/api/v1/categorias/{id}", id))
                .andExpect(status().isOk()).andReturn();
        assertEquals(nome, JsonPath.read(busca.getResponse().getContentAsString(), "$.nome"));
        assertEquals("SAIDA", JsonPath.read(busca.getResponse().getContentAsString(), "$.tipo"));

        MvcResult lista = mvc.perform(get("/api/v1/categorias").param("tipo", "SAIDA"))
                .andExpect(status().isOk()).andReturn();
        assertTrue(contemId(lista.getResponse().getContentAsString(), id));

        String novoNome = "Atualizada " + UUID.randomUUID();
        MvcResult atualizacao = mvc.perform(put("/api/v1/categorias/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoriaJson(novoNome, "Y", "SAIDA")))
                .andExpect(status().isOk()).andReturn();
        assertEquals(novoNome, JsonPath.read(atualizacao.getResponse().getContentAsString(), "$.nome"));
        assertEquals("Y", JsonPath.read(atualizacao.getResponse().getContentAsString(), "$.icone"));

        mvc.perform(delete("/api/v1/categorias/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/categorias/{id}", id))
                .andExpect(status().isNotFound());
        MvcResult listaDepois = mvc.perform(get("/api/v1/categorias").param("tipo", "SAIDA"))
                .andExpect(status().isOk()).andReturn();
        assertFalse(contemId(listaDepois.getResponse().getContentAsString(), id));
    }

    @Test
    void simuladoresPublicosCalculamEValidamDados() throws Exception {
        MvcResult juros = mvc.perform(post("/api/v1/educacao/simuladores/juros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorInicial\":100.00,\"taxaMensal\":10,\"meses\":2}"))
                .andExpect(status().isOk()).andReturn();
        assertValor("121.00", JsonPath.read(juros.getResponse().getContentAsString(), "$.montante"));
        assertValor("21.00", JsonPath.read(juros.getResponse().getContentAsString(), "$.jurosTotais"));

        MvcResult poupanca = mvc.perform(post("/api/v1/educacao/simuladores/poupanca")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aporteMensal\":100.00,\"taxaMensal\":10,\"meses\":3}"))
                .andExpect(status().isOk()).andReturn();
        assertValor("300.00", JsonPath.read(poupanca.getResponse().getContentAsString(), "$.totalAportado"));
        assertValor("331.00", JsonPath.read(poupanca.getResponse().getContentAsString(), "$.saldoFinal"));
        assertValor("31.00", JsonPath.read(poupanca.getResponse().getContentAsString(), "$.rendimento"));

        mvc.perform(post("/api/v1/educacao/simuladores/juros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorInicial\":100.00,\"taxaMensal\":10,\"meses\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/educacao/simuladores/poupanca")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aporteMensal\":100.00,\"taxaMensal\":-1,\"meses\":3}"))
                .andExpect(status().isBadRequest());
    }

    private String tokenAdmin() throws Exception {
        String telefone = "119" + String.format("%08d", 20000000 + Math.abs(UUID.randomUUID().hashCode() % 70000000));
        MvcResult cadastro = mvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Admin Teste\",\"telefone\":\"" + telefone
                                + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        long id = ((Number) JsonPath.read(cadastro.getResponse().getContentAsString(), "$.usuario.id")).longValue();
        Usuario usuario = usuarios.findById(id).orElseThrow();
        usuario.setPapel(Papel.ADMIN);
        usuarios.saveAndFlush(usuario);
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"" + telefone + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.token");
    }

    private String categoriaJson(String nome, String icone, String tipo) {
        return "{\"nome\":\"" + nome + "\",\"icone\":\"" + icone + "\",\"tipo\":\"" + tipo + "\"}";
    }

    private boolean contemId(String json, long id) {
        List<Number> ids = JsonPath.read(json, "$[*].id");
        return ids.stream().anyMatch(valor -> valor.longValue() == id);
    }

    private void assertValor(String esperado, Object atual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(new BigDecimal(atual.toString())));
    }
}
