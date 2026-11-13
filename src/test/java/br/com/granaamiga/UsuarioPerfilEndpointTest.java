package br.com.granaamiga;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:usuario_perfil_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "APP_JWT_SECRET=segredo-de-teste-do-perfil-com-mais-de-trinta-e-dois-bytes"
})
class UsuarioPerfilEndpointTest {

    @Autowired private MockMvc mvc;

    @Test
    void perfilExigeTokenERetornaDadosSemHashDaSenha() throws Exception {
        mvc.perform(get("/api/v1/usuarios/me"))
                .andExpect(status().isUnauthorized());

        String cadastro = """
                {"nome":"Pessoa Teste","telefone":"11 98765-4321", "senha":"SenhaForte123!", "rendaMensal":2345.67}
                """;
        MvcResult criacao = mvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON).content(cadastro))
                .andExpect(status().isCreated()).andReturn();
        String auth = criacao.getResponse().getContentAsString();
        String token = JsonPath.read(auth, "$.token");
        Number id = JsonPath.read(auth, "$.usuario.id");

        MvcResult perfil = mvc.perform(get("/api/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        String body = perfil.getResponse().getContentAsString();
        Map<String, Object> dados = JsonPath.parse(body).read("$");
        assertEquals(Set.of("id", "nome", "telefone", "papel", "rendaMensal", "dataCadastro"),
                dados.keySet());
        assertEquals(id.longValue(), ((Number) dados.get("id")).longValue());
        assertEquals("Pessoa Teste", dados.get("nome"));
        assertEquals("11987654321", dados.get("telefone"));
        assertEquals("USER", dados.get("papel"));
        assertEquals(0, new BigDecimal("2345.67").compareTo(new BigDecimal(dados.get("rendaMensal").toString())));
        assertNotNull(LocalDateTime.parse((String) dados.get("dataCadastro")));
    }
}
