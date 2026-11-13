package br.com.granaamiga.controller;

import br.com.granaamiga.model.SimulacaoJurosRequest;
import br.com.granaamiga.model.SimulacaoJurosResponse;
import br.com.granaamiga.model.SimulacaoPoupancaRequest;
import br.com.granaamiga.model.SimulacaoPoupancaResponse;
import br.com.granaamiga.service.EducacaoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/educacao")
public class EducacaoController {

    private final EducacaoService educacaoService;

    public EducacaoController(EducacaoService educacaoService) {
        this.educacaoService = educacaoService;
    }

    @PostMapping("/simuladores/juros")
    public SimulacaoJurosResponse simularJuros(@Valid @RequestBody SimulacaoJurosRequest request) {
        return educacaoService.simularJuros(request);
    }

    @PostMapping("/simuladores/poupanca")
    public SimulacaoPoupancaResponse simularPoupanca(@Valid @RequestBody SimulacaoPoupancaRequest request) {
        return educacaoService.simularPoupanca(request);
    }
}
