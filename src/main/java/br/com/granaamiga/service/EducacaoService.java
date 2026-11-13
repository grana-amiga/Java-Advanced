package br.com.granaamiga.service;

import br.com.granaamiga.model.educacao.SimulacaoJurosRequest;
import br.com.granaamiga.model.educacao.SimulacaoJurosResponse;
import br.com.granaamiga.model.educacao.SimulacaoPoupancaRequest;
import br.com.granaamiga.model.educacao.SimulacaoPoupancaResponse;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EducacaoService {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final MathContext PRECISAO = MathContext.DECIMAL128;

    public SimulacaoJurosResponse simularJuros(SimulacaoJurosRequest request) {
        validarLimites(request.valorInicial(), request.taxaMensal());
        BigDecimal fatorMensal = fatorMensal(request.taxaMensal());
        BigDecimal montante = request.valorInicial()
                .multiply(fatorMensal.pow(request.meses(), PRECISAO), PRECISAO);
        BigDecimal montanteEmReais = reais(montante);
        BigDecimal jurosTotais = montanteEmReais.subtract(reais(request.valorInicial()));
        return new SimulacaoJurosResponse(montanteEmReais, jurosTotais, "BRL");
    }

    public SimulacaoPoupancaResponse simularPoupanca(SimulacaoPoupancaRequest request) {
        validarLimites(request.aporteMensal(), request.taxaMensal());
        BigDecimal fatorMensal = fatorMensal(request.taxaMensal());
        BigDecimal saldo = BigDecimal.ZERO;

        // O aporte entra ao fim de cada mês, depois do rendimento do saldo anterior.
        for (int mes = 0; mes < request.meses(); mes++) {
            saldo = saldo.multiply(fatorMensal, PRECISAO).add(request.aporteMensal(), PRECISAO);
        }

        BigDecimal totalAportado = reais(request.aporteMensal().multiply(BigDecimal.valueOf(request.meses())));
        BigDecimal saldoFinal = reais(saldo);
        BigDecimal rendimento = saldoFinal.subtract(totalAportado);
        return new SimulacaoPoupancaResponse(totalAportado, saldoFinal, rendimento, "BRL");
    }

    private static BigDecimal fatorMensal(BigDecimal taxaPercentual) {
        return BigDecimal.ONE.add(taxaPercentual.divide(CEM, PRECISAO));
    }

    private static void validarLimites(BigDecimal valor, BigDecimal taxa) {
        if (valor.compareTo(new BigDecimal("1000000000.00")) > 0
                || taxa.compareTo(new BigDecimal("100")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Valor máximo de R$ 1 bilhão e taxa máxima de 100% ao mês");
        }
    }

    private static BigDecimal reais(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
