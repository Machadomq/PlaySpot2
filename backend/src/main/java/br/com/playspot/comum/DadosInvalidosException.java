package br.com.playspot.comum;

import org.springframework.http.HttpStatus;

public class DadosInvalidosException extends ErroDeNegocio {

    public DadosInvalidosException(String mensagem) {
        super(HttpStatus.BAD_REQUEST, mensagem);
    }
}
