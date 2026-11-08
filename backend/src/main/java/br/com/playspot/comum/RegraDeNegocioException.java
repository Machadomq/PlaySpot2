package br.com.playspot.comum;

import org.springframework.http.HttpStatus;

/** O pedido é válido, mas o estado atual do sistema não permite atendê-lo. */
public class RegraDeNegocioException extends ErroDeNegocio {

    public RegraDeNegocioException(String mensagem) {
        super(HttpStatus.CONFLICT, mensagem);
    }
}
