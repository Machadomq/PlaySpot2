package br.com.playspot.horario;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

/** horaInicio e horaFim estão no fuso da empresa; inicio e fim são o instante exato. */
public record HorarioDisponivelResponse(
        Instant inicio,
        Instant fim,
        LocalTime horaInicio,
        LocalTime horaFim,
        BigDecimal preco,
        boolean livre) {
}
