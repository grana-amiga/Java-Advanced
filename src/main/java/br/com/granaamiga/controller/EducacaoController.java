package br.com.granaamiga.controller;

import br.com.granaamiga.model.educacao.SimulacaoJurosRequest;
import br.com.granaamiga.model.educacao.SimulacaoJurosResponse;
import br.com.granaamiga.model.educacao.SimulacaoPoupancaRequest;
import br.com.granaamiga.model.educacao.SimulacaoPoupancaResponse;
import br.com.granaamiga.service.EducacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/educacao")
@Tag(name = "Educação financeira", description = "Simuladores públicos de juros e poupança")
public class EducacaoController {

    private final EducacaoService educacaoService;

    public EducacaoController(EducacaoService educacaoService) {
        this.educacaoService = educacaoService;
    }

    @PostMapping("/simuladores/juros")
    @Operation(summary = "Simular juros compostos", description = "Calcula o montante e os juros a partir de capital, taxa mensal e prazo, sem salvar dados.")
    public SimulacaoJurosResponse simularJuros(@Valid @RequestBody SimulacaoJurosRequest request) {
        return educacaoService.simularJuros(request);
    }

    @PostMapping("/simuladores/poupanca")
    @Operation(summary = "Simular poupança", description = "Projeta o saldo com rendimento mensal e aportes no fim de cada mês, sem salvar dados.")
    public SimulacaoPoupancaResponse simularPoupanca(@Valid @RequestBody SimulacaoPoupancaRequest request) {
        return educacaoService.simularPoupanca(request);
    }
}
