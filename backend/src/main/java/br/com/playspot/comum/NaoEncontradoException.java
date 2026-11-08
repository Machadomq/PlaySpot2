package br.com.playspot.comum;

import org.springframework.http.HttpStatus;

public class NaoEncontradoException extends ErroDeNegocio {

    public NaoEncontradoException(String mensagem) {
        super(HttpStatus.NOT_FOUND, mensagem);
    }
}
