package br.com.playspot.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EmailJaCadastradoException extends RuntimeException {

    public EmailJaCadastradoException() {
        super("E-mail já cadastrado");
    }
}
