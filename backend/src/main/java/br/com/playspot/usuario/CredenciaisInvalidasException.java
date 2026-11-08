package br.com.playspot.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        // mesma mensagem para e-mail inexistente e senha errada, para não revelar quais e-mails existem
        super("E-mail ou senha inválidos");
    }
}
