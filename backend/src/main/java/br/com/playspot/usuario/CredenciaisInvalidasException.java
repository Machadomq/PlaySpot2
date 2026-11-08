package br.com.playspot.usuario;

import org.springframework.http.HttpStatus;

import br.com.playspot.comum.ErroDeNegocio;

public class CredenciaisInvalidasException extends ErroDeNegocio {

    public CredenciaisInvalidasException() {
        // mesma mensagem para e-mail inexistente e senha errada, para não revelar quais e-mails existem
        super(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
    }
}
