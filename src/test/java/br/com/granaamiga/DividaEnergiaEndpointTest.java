package br.com.granaamiga;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:debt_energy_endpoint_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "APP_JWT_SECRET=segredo-de-teste-para-endpoints-de-dividas-e-energia"
})
class DividaEnergiaEndpointTest {

    private static final AtomicInteger TELEFONES = new AtomicInteger();
    private static final String SENHA = "SenhaForte123!";

    @Autowired
    private MockMvc mvc;

    @Test
    void dividaPodeSerListadaConsultadaAtualizadaEExcluida() throws Exception {
        String token = cadastrar();
        String body = dividaJson("Banco A", "100.00", "20.00");
        MvcResult criada = mvc.perform(post("/api/v1/dividas")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        long id = id(criada);
        assertEquals("Banco A", JsonPath.read(criada.getResponse().getContentAsString(), "$.credor"));

        MvcResult lista = mvc.perform(get("/api/v1/dividas")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        assertEquals(1, ((List<?>) JsonPath.read(lista.getResponse().getContentAsString(), "$")).size());

        MvcResult consulta = mvc.perform(get("/api/v1/dividas/{id}", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        assertEquals(id, ((Number) JsonPath.read(consulta.getResponse().getContentAsString(), "$.id")).longValue());

        MvcResult atualizada = mvc.perform(put("/api/v1/dividas/{id}", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dividaJson("Banco B", "80.00", "30.00")))
                .andExpect(status().isOk()).andReturn();
        assertEquals("Banco B", JsonPath.read(atualizada.getResponse().getContentAsString(), "$.credor"));
        assertDinheiro("80.00", JsonPath.read(atualizada.getResponse().getContentAsString(), "$.saldoDevedor"));

        MvcResult plano = mvc.perform(get("/api/v1/dividas/{id}/plano", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        assertTrue(Boolean.TRUE.equals(JsonPath.read(plano.getResponse().getContentAsString(), "$.quitavel")));

        mvc.perform(delete("/api/v1/dividas/{id}", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/dividas/{id}", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void dispositivoPodeSerListadoConsultadoAtualizadoEExcluido() throws Exception {
        String token = cadastrar();
        String codigo = "energia-" + UUID.randomUUID();
        MvcResult criado = mvc.perform(post("/api/v1/dispositivos")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dispositivoJson(codigo, "0.750000", "50.00")))
                .andExpect(status().isCreated()).andReturn();
        long id = id(criado);

        MvcResult lista = mvc.perform(get("/api/v1/dispositivos")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        assertEquals(1, ((List<?>) JsonPath.read(lista.getResponse().getContentAsString(), "$")).size());

        MvcResult consulta = mvc.perform(get("/api/v1/dispositivos/{id}", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        assertEquals(codigo, JsonPath.read(consulta.getResponse().getContentAsString(), "$.codigo"));

        String novoCodigo = "energia-" + UUID.randomUUID();
        MvcResult atualizado = mvc.perform(put("/api/v1/dispositivos/{id}", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dispositivoJson(novoCodigo, "1.000000", "30.00")))
                .andExpect(status().isOk()).andReturn();
        assertEquals(novoCodigo, JsonPath.read(atualizado.getResponse().getContentAsString(), "$.codigo"));
        assertDinheiro("30.00", JsonPath.read(atualizado.getResponse().getContentAsString(), "$.metaMensalReais"));

        mvc.perform(delete("/api/v1/dispositivos/{id}", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/dispositivos/{id}", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void leituraPodeSerConsultadaEResumidaEImpedeExcluirDispositivo() throws Exception {
        String token = cadastrar();
        MvcResult dispositivo = mvc.perform(post("/api/v1/dispositivos")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dispositivoJson("energia-" + UUID.randomUUID(), "0.500000", "10.00")))
                .andExpect(status().isCreated()).andReturn();
        long dispositivoId = id(dispositivo);

        String leituraBody = """
                {"dataLeitura":"2026-11-05T10:00:00","potenciaW":100.000,"kwh":2.000000}
                """;
        MvcResult leitura = mvc.perform(post("/api/v1/dispositivos/{id}/leituras", dispositivoId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(leituraBody))
                .andExpect(status().isCreated()).andReturn();
        long leituraId = id(leitura);
        assertDinheiro("1.00", JsonPath.read(leitura.getResponse().getContentAsString(), "$.custoEstimado"));

        MvcResult consulta = mvc.perform(get("/api/v1/dispositivos/{id}/leituras/{leituraId}",
                        dispositivoId, leituraId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        assertEquals(leituraId, ((Number) JsonPath.read(consulta.getResponse().getContentAsString(), "$.id")).longValue());

        MvcResult lista = mvc.perform(get("/api/v1/dispositivos/{id}/leituras", dispositivoId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        assertEquals(1, ((Number) JsonPath.read(lista.getResponse().getContentAsString(), "$.totalElementos")).intValue());

        MvcResult resumo = mvc.perform(get("/api/v1/dispositivos/{id}/resumo", dispositivoId)
                        .param("ano", "2026").param("mes", "11")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        assertDinheiro("1.00", JsonPath.read(resumo.getResponse().getContentAsString(), "$.custoTotal"));

        mvc.perform(delete("/api/v1/dispositivos/{id}", dispositivoId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/dispositivos/{id}", dispositivoId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    private String cadastrar() throws Exception {
        String telefone = String.format("119%08d", TELEFONES.incrementAndGet());
        String body = "{\"nome\":\"Pessoa Teste\",\"telefone\":\"" + telefone
                + "\",\"senha\":\"" + SENHA + "\"}";
        MvcResult resposta = mvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(resposta.getResponse().getContentAsString(), "$.token");
    }

    private String dividaJson(String credor, String saldo, String parcela) {
        return "{\"credor\":\"" + credor + "\",\"tipo\":\"CARTAO\",\"saldoDevedor\":"
                + saldo + ",\"taxaJurosMes\":2.0000,\"valorParcela\":"
                + parcela + ",\"diaVencimento\":10}";
    }

    private String dispositivoJson(String codigo, String tarifa, String meta) {
        return "{\"codigo\":\"" + codigo + "\",\"tarifaKwh\":" + tarifa
                + ",\"metaMensalReais\":" + meta + "}";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private long id(MvcResult resultado) throws Exception {
        return ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private void assertDinheiro(String esperado, Object atual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(new BigDecimal(atual.toString())));
    }
}
