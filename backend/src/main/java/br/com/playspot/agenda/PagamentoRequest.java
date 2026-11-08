package br.com.playspot.agenda;

import br.com.playspot.reserva.MetodoPagamento;
import jakarta.validation.constraints.NotNull;

public record PagamentoRequest(@NotNull(message = "Escolha a forma de pagamento.") MetodoPagamento metodo) {
}
