package br.com.playspot.comum;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Formato único de erro da API (ProblemDetail): o campo "detail" traz a mensagem para o usuário. */
@RestControllerAdvice
public class TratadorDeErros extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ErroDeNegocio.class)
    ProblemDetail erroDeNegocio(ErroDeNegocio erro) {
        return ProblemDetail.forStatusAndDetail(erro.getStatus(), erro.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException erro, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        erro.getBindingResult().getFieldErrors()
                .forEach(campo -> campos.putIfAbsent(campo.getField(), campo.getDefaultMessage()));

        ProblemDetail corpo = ProblemDetail.forStatusAndDetail(status, "Confira os dados informados.");
        corpo.setProperty("campos", campos);
        return handleExceptionInternal(erro, corpo, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException erro, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail corpo = ProblemDetail.forStatusAndDetail(status, "Dados inválidos na requisição.");
        return handleExceptionInternal(erro, corpo, headers, status, request);
    }
}
