package br.com.playspot.comum;

import org.springframework.http.HttpStatus;

/** Erro previsto pelas regras do produto; a mensagem é exibida ao usuário. */
public abstract class ErroDeNegocio extends RuntimeException {

    private final HttpStatus status;

    protected ErroDeNegocio(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
