package br.com.granaamiga.exception;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validar(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(erro ->
                campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        return erro(HttpStatus.BAD_REQUEST, "Dados inválidos", campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> validarParametro(ConstraintViolationException ex) {
        return erro(HttpStatus.BAD_REQUEST, "Parâmetros inválidos", Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> jsonInvalido(HttpMessageNotReadableException ex) {
        return erro(HttpStatus.BAD_REQUEST, "JSON inválido ou valor de enum desconhecido", Map.of());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> parametroInvalido(Exception ex) {
        return erro(HttpStatus.BAD_REQUEST, "Parâmetro ausente ou inválido", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> conflito(DataIntegrityViolationException ex) {
        return erro(HttpStatus.CONFLICT, "Registro em uso ou valor duplicado", Map.of());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> status(ResponseStatusException ex) {
        int codigo = ex.getStatusCode().value();
        String nome = HttpStatus.valueOf(codigo).getReasonPhrase();
        return ResponseEntity.status(codigo).body(new ApiError(Instant.now(), codigo, nome,
                ex.getReason() == null ? nome : ex.getReason(), Map.of()));
    }

    private ResponseEntity<ApiError> erro(HttpStatus status, String mensagem, Map<String, String> campos) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(),
                status.getReasonPhrase(), mensagem, campos));
    }
}
