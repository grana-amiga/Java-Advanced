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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:divida_indicadores_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "APP_JWT_SECRET=segredo-de-teste-para-indicadores-financeiros-2026"
})
class DividaIndicadoresEndpointTest {

    @Autowired private MockMvc mvc;

    @Test
    void termometroUsaParcelasAbertasERespeitaLimitesDoPlano() throws Exception {
        String token = cadastrar("1000.00");
        mvc.perform(get("/api/v1/dividas/termometro").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faixa").value("VERDE"))
                .andExpect(jsonPath("$.percentualComprometimento").value(0));

        criarDivida(token, "Banco Verde", "300.00", 31, "ABERTA", "1000.00");
        mvc.perform(get("/api/v1/dividas/termometro").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faixa").value("VERDE"))
                .andExpect(jsonPath("$.percentualComprometimento").value(30.00));

        criarDivida(token, "Banco Amarelo", "200.00", 15, "ABERTA", "1000.00");
        mvc.perform(get("/api/v1/dividas/termometro").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faixa").value("AMARELO"))
                .andExpect(jsonPath("$.percentualComprometimento").value(50.00));

        criarDivida(token, "Banco Vermelho", "0.01", 1, "ABERTA", "1000.00");
        criarDivida(token, "Banco Quitado", "500.00", 28, "QUITADA", "0.00");
        mvc.perform(get("/api/v1/dividas/termometro").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faixa").value("VERMELHO"))
                .andExpect(jsonPath("$.totalParcelasAbertas").value(500.01));

        mvc.perform(get("/api/v1/dividas/alertas")
                        .header("Authorization", bearer(token))
                        .param("dataReferencia", "2026-02-27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].credor").value("Banco Verde"))
                .andExpect(jsonPath("$[0].dataVencimento").value("2026-02-28"))
                .andExpect(jsonPath("$[0].diasRestantes").value(1))
                .andExpect(jsonPath("$[0].venceNestaSemana").value(true))
                .andExpect(jsonPath("$[1].dataVencimento").value("2026-03-01"));

        mvc.perform(get("/api/v1/dividas/alertas")
                        .header("Authorization", bearer(token))
                        .param("dataReferencia", "2026-02-28")
                        .param("dias", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].credor").value("Banco Verde"));
    }

    @Test
    void termometroSemRendaNaoCalculaPercentual() throws Exception {
        String token = cadastrar(null);
        mvc.perform(get("/api/v1/dividas/termometro").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faixa").value("SEM_RENDA"))
                .andExpect(jsonPath("$.percentualComprometimento").doesNotExist());
        mvc.perform(get("/api/v1/dividas/alertas").param("dias", "-1")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    private String cadastrar(String renda) throws Exception {
        String telefone = "119" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                .replaceAll("[a-f]", "1");
        String body = "{\"nome\":\"Pessoa Teste\",\"telefone\":\"" + telefone
                + "\",\"senha\":\"SenhaForte123!\""
                + (renda == null ? "" : ",\"rendaMensal\":" + renda) + "}";
        String json = mvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private void criarDivida(String token, String credor, String parcela, int dia,
            String statusDivida, String saldo) throws Exception {
        String body = "{\"credor\":\"" + credor + "\",\"tipo\":\"CARTAO\","
                + "\"saldoDevedor\":" + saldo + ",\"taxaJurosMes\":1.0,"
                + "\"valorParcela\":" + parcela + ",\"diaVencimento\":" + dia
                + ",\"status\":\"" + statusDivida + "\"}";
        mvc.perform(post("/api/v1/dividas").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
