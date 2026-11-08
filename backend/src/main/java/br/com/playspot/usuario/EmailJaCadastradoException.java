package br.com.playspot.usuario;

import org.springframework.http.HttpStatus;

import br.com.playspot.comum.ErroDeNegocio;

public class EmailJaCadastradoException extends ErroDeNegocio {

    public EmailJaCadastradoException() {
        super(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
    }
}
