package br.com.granaamiga;

import com.jayway.jsonpath.JsonPath;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_bootstrap_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "APP_JWT_SECRET=segredo-de-teste-admin-com-mais-de-trinta-e-dois-bytes",
        "APP_BOOTSTRAP_ADMIN_PHONE=11999997777",
        "APP_BOOTSTRAP_ADMIN_PASSWORD=senha-admin-teste-123"
})
class AdminBootstrapIntegrationTest {

    @Autowired private MockMvc mvc;

    @Test
    void administradorInicialConsegueEntrarEManterCategorias() throws Exception {
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"11999997777\",\"senha\":\"senha-admin-teste-123\"}"))
                .andExpect(status().isOk()).andReturn();
        String json = login.getResponse().getContentAsString();
        assertEquals("ADMIN", JsonPath.read(json, "$.usuario.papel"));
        String token = JsonPath.read(json, "$.token");

        mvc.perform(post("/api/v1/categorias")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Teste " + UUID.randomUUID()
                                + "\",\"icone\":\"check\",\"tipo\":\"SAIDA\"}"))
                .andExpect(status().isCreated());
    }
}
