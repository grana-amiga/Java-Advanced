package br.com.citizentech.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(Instant instante, int status, String erro, String mensagem,
                       Map<String, String> campos) {
}
