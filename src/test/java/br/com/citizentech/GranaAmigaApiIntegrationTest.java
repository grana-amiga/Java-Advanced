package br.com.citizentech;

import br.com.citizentech.domain.Categoria;
import br.com.citizentech.domain.Papel;
import br.com.citizentech.domain.TipoTransacao;
import br.com.citizentech.domain.Usuario;
import br.com.citizentech.repository.CategoriaRepository;
import br.com.citizentech.repository.DispositivoRepository;
import br.com.citizentech.repository.DividaRepository;
import br.com.citizentech.repository.LeituraEnergiaRepository;
import br.com.citizentech.repository.TransacaoRepository;
import br.com.citizentech.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:grana_amiga_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cache.type=caffeine",
        "APP_JWT_SECRET=segredo-de-integracao-com-mais-de-trinta-e-dois-bytes"
})
class GranaAmigaApiIntegrationTest {

    private static final String SENHA = "SenhaForte123!";
    private static final AtomicInteger TELEFONES = new AtomicInteger();

    @Autowired private MockMvc mvc;
    @Autowired private JwtDecoder jwtDecoder;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private CategoriaRepository categorias;
    @Autowired private TransacaoRepository transacoes;
    @Autowired private DividaRepository dividas;
    @Autowired private DispositivoRepository dispositivos;
    @Autowired private LeituraEnergiaRepository leituras;
    @Autowired private CacheManager cacheManager;

    @Test
    void cadastroLoginNormalizacaoJwtEDuplicidade() throws Exception {
        Conta conta = cadastrar("Ana Teste");
        String formatado = conta.telefone().substring(0, 2) + " "
                + conta.telefone().substring(2, 7) + "-" + conta.telefone().substring(7);
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"" + formatado + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonPath.read(login.getResponse().getContentAsString(), "$.token");
        assertEquals(3, token.split("\\.").length);
        Jwt jwt = jwtDecoder.decode(token);
        assertEquals(conta.id().toString(), jwt.getSubject());
        assertEquals(List.of("USER"), jwt.getClaimAsStringList("roles"));
        assertTrue(jwt.getExpiresAt().isAfter(jwt.getIssuedAt()));

        mvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroJson("Outra Ana", conta.telefone())))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"" + conta.telefone() + "\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroJson("Renda Invalida", "11999999999")
                                .replace("2000.00", "2000.001")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acessoSemTokenEPermissoesPorPapel() throws Exception {
        mvc.perform(get("/api/v1/transacoes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/categorias")).andExpect(status().isOk());
        MvcResult swagger = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn();
        assertTrue(swagger.getResponse().getContentAsString().contains("\"/api/v1/transacoes\""));

        Conta conta = cadastrar("Usuario Comum");
        String categoriaJson = "{\"nome\":\"Categoria " + UUID.randomUUID()
                + "\",\"icone\":\"X\",\"tipo\":\"SAIDA\"}";
        mvc.perform(post("/api/v1/categorias")
                        .header("Authorization", bearer(conta))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoriaJson))
                .andExpect(status().isForbidden());

        Usuario admin = usuarios.findByTelefone(conta.telefone()).orElseThrow();
        admin.setPapel(Papel.ADMIN);
        usuarios.saveAndFlush(admin);
        String tokenAdmin = entrar(conta.telefone(), SENHA);
        mvc.perform(post("/api/v1/categorias")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoriaJson))
                .andExpect(status().isCreated());
    }

    @Test
    void tokenComPapelAntigoNaoMantemAcessoAdministrativo() throws Exception {
        Conta conta = cadastrar("Papel Alterado");
        Usuario usuario = usuarios.findById(conta.id()).orElseThrow();
        usuario.setPapel(Papel.ADMIN);
        usuarios.saveAndFlush(usuario);
        String tokenAdmin = entrar(conta.telefone(), SENHA);
        String categoriaAdmin = "{\"nome\":\"Admin " + UUID.randomUUID()
                + "\",\"icone\":\"X\",\"tipo\":\"SAIDA\"}";
        mvc.perform(post("/api/v1/categorias")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoriaAdmin))
                .andExpect(status().isCreated());

        usuario.setPapel(Papel.USER);
        usuarios.saveAndFlush(usuario);
        mvc.perform(post("/api/v1/categorias")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Negada " + UUID.randomUUID()
                                + "\",\"icone\":\"X\",\"tipo\":\"SAIDA\"}"))
                .andExpect(status().isUnauthorized());
        String tokenUser = entrar(conta.telefone(), SENHA);
        mvc.perform(get("/api/v1/transacoes")
                        .header("Authorization", "Bearer " + tokenUser))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/categorias")
                        .header("Authorization", "Bearer " + tokenUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Sem Permissao " + UUID.randomUUID()
                                + "\",\"icone\":\"X\",\"tipo\":\"SAIDA\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void transacoesEDividasSaoIsoladasPorUsuario() throws Exception {
        Conta ana = cadastrar("Ana Isolamento");
        Conta bia = cadastrar("Bia Isolamento");
        Categoria mercado = novaCategoria(TipoTransacao.SAIDA);

        long transacaoId = id(criarTransacao(ana, mercado.getId(), "SAIDA", "25.00", "2026-11-05T10:00:00"));
        long dividaId = id(criarDivida(ana, "100.00", "10.0000", "20.00"));

        mvc.perform(get("/api/v1/transacoes/{id}", transacaoId).header("Authorization", bearer(bia)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/transacoes/{id}", transacaoId)
                        .header("Authorization", bearer(bia))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transacaoJson(mercado.getId(), "SAIDA", "30.00", "2026-11-05T10:00:00")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/transacoes/{id}", transacaoId).header("Authorization", bearer(bia)))
                .andExpect(status().isNotFound());
        MvcResult listaTransacoes = mvc.perform(get("/api/v1/transacoes").header("Authorization", bearer(bia)))
                .andExpect(status().isOk()).andReturn();
        assertTrue(((List<?>) JsonPath.read(listaTransacoes.getResponse().getContentAsString(), "$.itens")).isEmpty());

        mvc.perform(get("/api/v1/dividas/{id}", dividaId).header("Authorization", bearer(bia)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/dividas/{id}/plano", dividaId).header("Authorization", bearer(bia)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/dividas/{id}", dividaId)
                        .header("Authorization", bearer(bia))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dividaJson("100.00", "10.0000", "20.00")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/dividas/{id}", dividaId).header("Authorization", bearer(bia)))
                .andExpect(status().isNotFound());
        MvcResult listaDividas = mvc.perform(get("/api/v1/dividas").header("Authorization", bearer(bia)))
                .andExpect(status().isOk()).andReturn();
        assertTrue(((List<?>) JsonPath.read(listaDividas.getResponse().getContentAsString(), "$")).isEmpty());

        mvc.perform(get("/api/v1/transacoes/{id}", transacaoId).header("Authorization", bearer(ana)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/dividas/{id}", dividaId).header("Authorization", bearer(ana)))
                .andExpect(status().isOk());
    }

    @Test
    void categoriaIncompativelEResumoMensal() throws Exception {
        Conta conta = cadastrar("Resumo Teste");
        Categoria entrada = novaCategoria(TipoTransacao.ENTRADA);
        Categoria saida = novaCategoria(TipoTransacao.SAIDA);

        mvc.perform(post("/api/v1/transacoes")
                        .header("Authorization", bearer(conta))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transacaoJson(entrada.getId(), "SAIDA", "5.00", "2026-11-05T10:00:00")))
                .andExpect(status().isBadRequest());

        criarTransacao(conta, entrada.getId(), "ENTRADA", "1000.00", "2026-11-05T10:00:00");
        criarTransacao(conta, saida.getId(), "SAIDA", "250.00", "2026-11-06T10:00:00");
        criarTransacao(conta, saida.getId(), "SAIDA", "50.00", "2026-10-06T10:00:00");
        MvcResult resumo = mvc.perform(get("/api/v1/transacoes/resumo")
                        .param("ano", "2026").param("mes", "11")
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isOk()).andReturn();
        String json = resumo.getResponse().getContentAsString();
        assertDinheiro("1000.00", JsonPath.read(json, "$.entradas"));
        assertDinheiro("250.00", JsonPath.read(json, "$.saidas"));
        assertDinheiro("750.00", JsonPath.read(json, "$.saldo"));
        assertEquals(1, ((List<?>) JsonPath.read(json, "$.entradasPorCategoria")).size());
        assertEquals(1, ((List<?>) JsonPath.read(json, "$.saidasPorCategoria")).size());
    }

    @Test
    void planoDeQuitacaoCalculaJurosEIdentificaParcelaInsuficiente() throws Exception {
        Conta conta = cadastrar("Plano Teste");
        long quitavelId = id(criarDivida(conta, "100.00", "10.0000", "110.00"));
        MvcResult divida = mvc.perform(get("/api/v1/dividas/{id}", quitavelId)
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isOk()).andReturn();
        assertEquals("/api/v1/dividas/" + quitavelId + "/plano",
                JsonPath.read(divida.getResponse().getContentAsString(), "$._links.plano.href"));
        MvcResult quitavel = mvc.perform(get("/api/v1/dividas/{id}/plano", quitavelId)
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isOk()).andReturn();
        String plano = quitavel.getResponse().getContentAsString();
        assertTrue((Boolean) JsonPath.read(plano, "$.quitavel"));
        assertEquals(1, ((Number) JsonPath.read(plano, "$.mesesParaQuitacao")).intValue());
        assertDinheiro("110.00", JsonPath.read(plano, "$.totalPagoSimulado"));
        assertDinheiro("10.00", JsonPath.read(plano, "$.totalJurosSimulados"));
        assertDinheiro("0.00", JsonPath.read(plano, "$.saldoRestante"));
        assertEquals(1, ((List<?>) JsonPath.read(plano, "$.primeirasParcelas")).size());

        long insuficienteId = id(criarDivida(conta, "100.00", "10.0000", "10.00"));
        MvcResult insuficiente = mvc.perform(get("/api/v1/dividas/{id}/plano", insuficienteId)
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isOk()).andReturn();
        String semQuitacao = insuficiente.getResponse().getContentAsString();
        assertFalse((Boolean) JsonPath.read(semQuitacao, "$.quitavel"));
        assertNull(JsonPath.read(semQuitacao, "$.mesesParaQuitacao"));
        assertEquals(12, ((Number) JsonPath.read(semQuitacao, "$.mesesSimulados")).intValue());
        assertEquals(12, ((List<?>) JsonPath.read(semQuitacao, "$.primeirasParcelas")).size());
        assertDinheiro("100.00", JsonPath.read(semQuitacao, "$.saldoRestante"));
    }

    @Test
    void atualizarTransacaoSemDataPreservaMesOriginal() throws Exception {
        Conta conta = cadastrar("Data Preservada");
        Categoria categoria = novaCategoria(TipoTransacao.SAIDA);
        long id = id(criarTransacao(conta, categoria.getId(), "SAIDA", "20.00", "2026-10-15T10:00:00"));

        String semData = "{\"categoriaId\":" + categoria.getId()
                + ",\"valor\":30.00,\"tipo\":\"SAIDA\",\"descricao\":\"Atualizada\"}";
        MvcResult atualizacao = mvc.perform(put("/api/v1/transacoes/{id}", id)
                        .header("Authorization", bearer(conta))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(semData))
                .andExpect(status().isOk()).andReturn();
        String data = JsonPath.read(atualizacao.getResponse().getContentAsString(), "$.dataTransacao");
        assertTrue(data.startsWith("2026-10-15T10:00"));

        MvcResult outubro = mvc.perform(get("/api/v1/transacoes/resumo")
                        .param("ano", "2026").param("mes", "10")
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isOk()).andReturn();
        assertDinheiro("30.00", JsonPath.read(outubro.getResponse().getContentAsString(), "$.saidas"));
        MvcResult novembro = mvc.perform(get("/api/v1/transacoes/resumo")
                        .param("ano", "2026").param("mes", "11")
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isOk()).andReturn();
        assertDinheiro("0.00", JsonPath.read(novembro.getResponse().getContentAsString(), "$.saidas"));
    }

    @Test
    void custoUmCentavoAcimaDaMetaFicaVermelho() throws Exception {
        Conta conta = cadastrar("Energia Faixa");
        long dispositivoId = criarDispositivo(conta, "1.000000", "100.00");
        registrarLeitura(conta, dispositivoId, "2026-11-05T10:00:00", "100.010000");

        MvcResult resumo = mvc.perform(get("/api/v1/dispositivos/{id}/resumo", dispositivoId)
                        .param("ano", "2026").param("mes", "11")
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isOk()).andReturn();
        String json = resumo.getResponse().getContentAsString();
        assertDinheiro("100.01", JsonPath.read(json, "$.custoTotal"));
        assertEquals("VERMELHO", JsonPath.read(json, "$.faixa"));
    }

    @Test
    void leiturasTemPaginacaoEOrdenacaoEstaveis() throws Exception {
        Conta conta = cadastrar("Energia Pagina");
        long dispositivoId = criarDispositivo(conta, "0.750000", "50.00");
        registrarLeitura(conta, dispositivoId, "2026-11-01T10:00:00", "1.000000");
        registrarLeitura(conta, dispositivoId, "2026-11-02T10:00:00", "1.000000");
        registrarLeitura(conta, dispositivoId, "2026-11-03T10:00:00", "1.000000");

        MvcResult primeira = mvc.perform(get("/api/v1/dispositivos/{id}/leituras", dispositivoId)
                        .param("pagina", "0").param("tamanho", "2")
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isOk()).andReturn();
        String paginaZero = primeira.getResponse().getContentAsString();
        assertEquals(3, ((Number) JsonPath.read(paginaZero, "$.totalElementos")).intValue());
        assertEquals(2, ((Number) JsonPath.read(paginaZero, "$.totalPaginas")).intValue());
        assertEquals(2, ((List<?>) JsonPath.read(paginaZero, "$.itens")).size());
        assertTrue(((String) JsonPath.read(paginaZero, "$.itens[0].dataLeitura")).startsWith("2026-11-03"));
        assertTrue(((String) JsonPath.read(paginaZero, "$.itens[1].dataLeitura")).startsWith("2026-11-02"));

        MvcResult segunda = mvc.perform(get("/api/v1/dispositivos/{id}/leituras", dispositivoId)
                        .param("pagina", "1").param("tamanho", "2")
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isOk()).andReturn();
        String paginaUm = segunda.getResponse().getContentAsString();
        assertEquals(1, ((List<?>) JsonPath.read(paginaUm, "$.itens")).size());
        assertTrue(((String) JsonPath.read(paginaUm, "$.itens[0].dataLeitura")).startsWith("2026-11-01"));

        mvc.perform(get("/api/v1/dispositivos/{id}/leituras", dispositivoId)
                        .param("pagina", "-1").param("tamanho", "2")
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dispositivoELeiturasNaoVazamParaOutraConta() throws Exception {
        Conta dona = cadastrar("Dona Energia");
        Conta outra = cadastrar("Outra Energia");
        long dispositivoId = criarDispositivo(dona, "0.750000", "50.00");
        long leituraId = registrarLeitura(dona, dispositivoId, "2026-11-05T10:00:00", "2.000000");
        String dispositivoBody = "{\"codigo\":\"outro-" + UUID.randomUUID()
                + "\",\"tarifaKwh\":0.750000,\"metaMensalReais\":50.00}";
        String leituraBody = "{\"dataLeitura\":\"2026-11-05T11:00:00\",\"potenciaW\":100.000,\"kwh\":1.000000}";

        mvc.perform(get("/api/v1/dispositivos/{id}", dispositivoId)
                        .header("Authorization", bearer(outra)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/dispositivos/{id}/leituras", dispositivoId)
                        .header("Authorization", bearer(outra)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/dispositivos/{id}/leituras/{leituraId}", dispositivoId, leituraId)
                        .header("Authorization", bearer(outra)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/dispositivos/{id}/resumo", dispositivoId)
                        .param("ano", "2026").param("mes", "11")
                        .header("Authorization", bearer(outra)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/dispositivos/{id}", dispositivoId)
                        .header("Authorization", bearer(outra))
                        .contentType(MediaType.APPLICATION_JSON).content(dispositivoBody))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/dispositivos/{id}/leituras", dispositivoId)
                        .header("Authorization", bearer(outra))
                        .contentType(MediaType.APPLICATION_JSON).content(leituraBody))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/dispositivos/{id}", dispositivoId)
                        .header("Authorization", bearer(outra)))
                .andExpect(status().isNotFound());
        MvcResult listaOutra = mvc.perform(get("/api/v1/dispositivos")
                        .header("Authorization", bearer(outra)))
                .andExpect(status().isOk()).andReturn();
        assertTrue(((List<?>) JsonPath.read(listaOutra.getResponse().getContentAsString(), "$"))
                .isEmpty());
        assertTrue(dispositivos.existsById(dispositivoId));
        assertTrue(leituras.existsById(leituraId));
    }

    @Test
    void dividaQuitadaExigeSaldoZero() throws Exception {
        Conta conta = cadastrar("Quitacao Teste");
        long id = id(criarDivida(conta, "100.00", "2.0000", "20.00"));
        mvc.perform(put("/api/v1/dividas/{id}", id)
                        .header("Authorization", bearer(conta))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dividaJson("100.00", "2.0000", "20.00").replace("}", ",\"status\":\"QUITADA\"}")))
                .andExpect(status().isBadRequest());

        MvcResult quitada = mvc.perform(put("/api/v1/dividas/{id}", id)
                        .header("Authorization", bearer(conta))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dividaJson("0.00", "2.0000", "20.00").replace("}", ",\"status\":\"QUITADA\"}")))
                .andExpect(status().isOk()).andReturn();
        assertEquals("QUITADA", JsonPath.read(quitada.getResponse().getContentAsString(), "$.status"));
        MvcResult plano = mvc.perform(get("/api/v1/dividas/{id}/plano", id)
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isOk()).andReturn();
        assertEquals(0, ((Number) JsonPath.read(plano.getResponse().getContentAsString(), "$.mesesParaQuitacao")).intValue());
    }

    @Test
    void categoriaUtilizadaNaoPodeTrocarDeTipo() throws Exception {
        Conta conta = cadastrar("Categoria Usada");
        Categoria categoria = novaCategoria(TipoTransacao.SAIDA);
        criarTransacao(conta, categoria.getId(), "SAIDA", "12.00", "2026-11-05T10:00:00");

        Usuario admin = usuarios.findById(conta.id()).orElseThrow();
        admin.setPapel(Papel.ADMIN);
        usuarios.saveAndFlush(admin);
        String tokenAdmin = entrar(conta.telefone(), SENHA);
        String body = "{\"nome\":\"" + categoria.getNome()
                + "\",\"icone\":\"X\",\"tipo\":\"ENTRADA\"}";
        mvc.perform(put("/api/v1/categorias/{id}", categoria.getId())
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
        assertEquals(TipoTransacao.SAIDA, categorias.findById(categoria.getId()).orElseThrow().getTipo());
    }

    @Test
    void excluirContaRemoveDadosERetiraAcessoDoTokenAntigo() throws Exception {
        Conta conta = cadastrar("Excluir Conta");
        Categoria categoria = novaCategoria(TipoTransacao.SAIDA);
        long transacaoId = id(criarTransacao(conta, categoria.getId(), "SAIDA", "15.00", "2026-11-05T10:00:00"));
        long dividaId = id(criarDivida(conta, "100.00", "1.0000", "20.00"));
        long dispositivoId = criarDispositivo(conta, "1.000000", "50.00");
        long leituraId = registrarLeitura(conta, dispositivoId, "2026-11-05T10:00:00", "5.000000");

        mvc.perform(delete("/api/v1/usuarios/me")
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isNoContent());
        assertFalse(usuarios.existsById(conta.id()));
        assertFalse(transacoes.existsById(transacaoId));
        assertFalse(dividas.existsById(dividaId));
        assertFalse(dispositivos.existsById(dispositivoId));
        assertFalse(leituras.existsById(leituraId));
        mvc.perform(get("/api/v1/transacoes").header("Authorization", bearer(conta)))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/usuarios/me").header("Authorization", bearer(conta)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroJson("Conta Nova", conta.telefone())))
                .andExpect(status().isCreated());
    }

    @Test
    void extratoMensalIncluiPrimeiroEUltimoInstanteDoMes() throws Exception {
        Conta conta = cadastrar("Extrato Limites");
        Conta outra = cadastrar("Outro Extrato");
        Categoria categoria = novaCategoria(TipoTransacao.SAIDA);
        criarTransacao(conta, categoria.getId(), "SAIDA", "10.00", "2026-10-31T23:59:59.999999");
        long primeiro = id(criarTransacao(conta, categoria.getId(), "SAIDA", "11.00", "2026-11-01T00:00:00"));
        long ultimo = id(criarTransacao(conta, categoria.getId(), "SAIDA", "12.00", "2026-11-30T23:59:59.999999"));
        criarTransacao(conta, categoria.getId(), "SAIDA", "13.00", "2026-12-01T00:00:00");
        criarTransacao(outra, categoria.getId(), "SAIDA", "14.00", "2026-11-15T12:00:00");

        MvcResult resposta = mvc.perform(get("/api/v1/transacoes/extrato")
                        .param("ano", "2026").param("mes", "11")
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isOk()).andReturn();
        String json = resposta.getResponse().getContentAsString();
        assertEquals(2, ((Number) JsonPath.read(json, "$.totalElementos")).intValue());
        assertEquals(ultimo, ((Number) JsonPath.read(json, "$.itens[0].id")).longValue());
        assertEquals(primeiro, ((Number) JsonPath.read(json, "$.itens[1].id")).longValue());
    }

    @Test
    void filtrosCombinadosPaginamSemPerderEmpates() throws Exception {
        Conta conta = cadastrar("Filtros Combinados");
        Categoria alvo = novaCategoria(TipoTransacao.SAIDA);
        Categoria outraCategoria = novaCategoria(TipoTransacao.SAIDA);
        Categoria entrada = novaCategoria(TipoTransacao.ENTRADA);
        long primeiro = id(criarTransacao(conta, alvo.getId(), "SAIDA", "20.00", "2026-11-01T10:00:00"));
        long segundo = id(criarTransacao(conta, alvo.getId(), "SAIDA", "20.00", "2026-11-02T10:00:00"));
        criarTransacao(conta, outraCategoria.getId(), "SAIDA", "20.00", "2026-11-02T10:00:00");
        criarTransacao(conta, alvo.getId(), "SAIDA", "20.00", "2026-11-03T10:00:00");
        criarTransacao(conta, entrada.getId(), "ENTRADA", "20.00", "2026-11-02T10:00:00");

        MvcResult paginaZero = mvc.perform(get("/api/v1/transacoes")
                        .param("tipo", "SAIDA").param("categoriaId", alvo.getId().toString())
                        .param("de", "2026-11-01").param("ate", "2026-11-02")
                        .param("valorMin", "20.00").param("valorMax", "20.00")
                        .param("pagina", "0").param("tamanho", "1")
                        .param("ordenarPor", "valor").param("direcao", "ASC")
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isOk()).andReturn();
        MvcResult paginaUm = mvc.perform(get("/api/v1/transacoes")
                        .param("tipo", "SAIDA").param("categoriaId", alvo.getId().toString())
                        .param("de", "2026-11-01").param("ate", "2026-11-02")
                        .param("valorMin", "20.00").param("valorMax", "20.00")
                        .param("pagina", "1").param("tamanho", "1")
                        .param("ordenarPor", "valor").param("direcao", "ASC")
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isOk()).andReturn();
        assertEquals(2, ((Number) JsonPath.read(paginaZero.getResponse().getContentAsString(), "$.totalElementos")).intValue());
        assertEquals(primeiro, ((Number) JsonPath.read(paginaZero.getResponse().getContentAsString(), "$.itens[0].id")).longValue());
        assertEquals(segundo, ((Number) JsonPath.read(paginaUm.getResponse().getContentAsString(), "$.itens[0].id")).longValue());
        mvc.perform(get("/api/v1/transacoes")
                        .param("valorMin", "30.00").param("valorMax", "20.00")
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resumoUsaCacheEInvalidaAposEditarTransacao() throws Exception {
        Conta conta = cadastrar("Cache Resumo");
        Categoria categoria = novaCategoria(TipoTransacao.SAIDA);
        long transacaoId = id(criarTransacao(conta, categoria.getId(), "SAIDA", "10.00", "2026-11-05T10:00:00"));
        CaffeineCache cache = (CaffeineCache) cacheManager.getCache("resumoMensal");
        assertTrue(cache != null);
        long acertosAntes = cache.getNativeCache().stats().hitCount();
        mvc.perform(get("/api/v1/transacoes/resumo").param("ano", "2026").param("mes", "11")
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/transacoes/resumo").param("ano", "2026").param("mes", "11")
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isOk());
        assertTrue(cache.getNativeCache().stats().hitCount() > acertosAntes);

        mvc.perform(put("/api/v1/transacoes/{id}", transacaoId)
                        .header("Authorization", bearer(conta))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transacaoJson(categoria.getId(), "SAIDA", "25.00", "2026-11-05T10:00:00")))
                .andExpect(status().isOk());
        MvcResult atualizado = mvc.perform(get("/api/v1/transacoes/resumo")
                        .param("ano", "2026").param("mes", "11")
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isOk()).andReturn();
        assertDinheiro("25.00", JsonPath.read(atualizado.getResponse().getContentAsString(), "$.saidas"));
    }

    @Test
    void resumoEmCacheSegueCriacaoExclusaoEMantemUsuariosSeparados() throws Exception {
        Conta ana = cadastrar("Cache Ana");
        Conta bia = cadastrar("Cache Bia");
        Categoria categoria = novaCategoria(TipoTransacao.SAIDA);
        long transacaoAna = id(criarTransacao(ana, categoria.getId(), "SAIDA", "10.00", "2026-11-05T10:00:00"));
        criarTransacao(bia, categoria.getId(), "SAIDA", "7.00", "2026-11-05T10:00:00");

        assertDinheiro("10.00", resumoSaidas(ana));
        assertDinheiro("7.00", resumoSaidas(bia));
        criarTransacao(ana, categoria.getId(), "SAIDA", "20.00", "2026-11-06T10:00:00");
        assertDinheiro("30.00", resumoSaidas(ana));
        assertDinheiro("7.00", resumoSaidas(bia));

        mvc.perform(delete("/api/v1/transacoes/{id}", transacaoAna)
                        .header("Authorization", bearer(ana)))
                .andExpect(status().isNoContent());
        assertDinheiro("20.00", resumoSaidas(ana));
        assertDinheiro("7.00", resumoSaidas(bia));
    }

    private Object resumoSaidas(Conta conta) throws Exception {
        MvcResult resultado = mvc.perform(get("/api/v1/transacoes/resumo")
                        .param("ano", "2026").param("mes", "11")
                        .header("Authorization", bearer(conta)))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.saidas");
    }

    private Conta cadastrar(String nome) throws Exception {
        String telefone = String.format("119%08d", TELEFONES.incrementAndGet());
        MvcResult resultado = mvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroJson(nome, telefone)))
                .andExpect(status().isCreated()).andReturn();
        String json = resultado.getResponse().getContentAsString();
        assertEquals("Bearer", JsonPath.read(json, "$.tipo"));
        assertEquals("USER", JsonPath.read(json, "$.usuario.papel"));
        return new Conta((String) JsonPath.read(json, "$.token"),
                ((Number) JsonPath.read(json, "$.usuario.id")).longValue(), telefone);
    }

    private String entrar(String telefone, String senha) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"" + telefone + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
    }

    private Categoria novaCategoria(TipoTransacao tipo) {
        Categoria categoria = new Categoria();
        categoria.setNome("Teste " + UUID.randomUUID());
        categoria.setIcone("X");
        categoria.setTipo(tipo);
        return categorias.saveAndFlush(categoria);
    }

    private String criarTransacao(Conta conta, Long categoriaId, String tipo, String valor, String data) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/v1/transacoes")
                        .header("Authorization", bearer(conta))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transacaoJson(categoriaId, tipo, valor, data)))
                .andExpect(status().isCreated()).andReturn();
        return resultado.getResponse().getContentAsString();
    }

    private String criarDivida(Conta conta, String saldo, String taxa, String parcela) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/v1/dividas")
                        .header("Authorization", bearer(conta))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dividaJson(saldo, taxa, parcela)))
                .andExpect(status().isCreated()).andReturn();
        return resultado.getResponse().getContentAsString();
    }

    private long criarDispositivo(Conta conta, String tarifa, String meta) throws Exception {
        String body = "{\"codigo\":\"teste-" + UUID.randomUUID() + "\",\"tarifaKwh\":" + tarifa
                + ",\"metaMensalReais\":" + meta + "}";
        MvcResult resultado = mvc.perform(post("/api/v1/dispositivos")
                        .header("Authorization", bearer(conta))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated()).andReturn();
        return id(resultado.getResponse().getContentAsString());
    }

    private long registrarLeitura(Conta conta, long dispositivoId, String data, String kwh) throws Exception {
        String body = "{\"dataLeitura\":\"" + data + "\",\"potenciaW\":100.000,\"kwh\":" + kwh + "}";
        MvcResult resultado = mvc.perform(post("/api/v1/dispositivos/{id}/leituras", dispositivoId)
                        .header("Authorization", bearer(conta))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated()).andReturn();
        return id(resultado.getResponse().getContentAsString());
    }

    private long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private void assertDinheiro(String esperado, Object atual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(new BigDecimal(atual.toString())));
    }

    private String bearer(Conta conta) {
        return "Bearer " + conta.token();
    }

    private String cadastroJson(String nome, String telefone) {
        return "{\"nome\":\"" + nome + "\",\"telefone\":\"" + telefone
                + "\",\"senha\":\"" + SENHA + "\",\"rendaMensal\":2000.00}";
    }

    private String transacaoJson(Long categoriaId, String tipo, String valor, String data) {
        return "{\"categoriaId\":" + categoriaId + ",\"valor\":" + valor
                + ",\"tipo\":\"" + tipo + "\",\"descricao\":\"Teste\",\"dataTransacao\":\"" + data + "\"}";
    }

    private String dividaJson(String saldo, String taxa, String parcela) {
        return "{\"credor\":\"Banco Teste\",\"tipo\":\"CARTAO\",\"saldoDevedor\":" + saldo
                + ",\"taxaJurosMes\":" + taxa + ",\"valorParcela\":" + parcela
                + ",\"diaVencimento\":10}";
    }

    private record Conta(String token, Long id, String telefone) {
    }
}
