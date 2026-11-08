package br.com.playspot.agenda;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import br.com.playspot.reserva.MetodoPagamento;
import br.com.playspot.reserva.StatusReserva;

/** Uma linha da agenda do dono. data, horaInicio e horaFim estão no fuso da empresa. */
public record AgendaItem(
        Long id,
        Long quadraId,
        String quadraNome,
        Instant inicio,
        Instant fim,
        LocalDate data,
        LocalTime horaInicio,
        LocalTime horaFim,
        String cliente,
        String telefone,
        boolean peloApp,
        BigDecimal preco,
        StatusReserva status,
        MetodoPagamento metodoPago) {
}
