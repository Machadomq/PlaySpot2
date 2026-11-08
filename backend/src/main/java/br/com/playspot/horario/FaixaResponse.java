package br.com.playspot.horario;

import java.math.BigDecimal;
import java.time.LocalTime;

public record FaixaResponse(short diaSemana, LocalTime horaInicio, LocalTime horaFim, BigDecimal precoHora) {

    static FaixaResponse de(Funcionamento faixa) {
        return new FaixaResponse(
                faixa.getDiaSemana(), faixa.getHoraInicio(), faixa.getHoraFim(), faixa.getPrecoHora());
    }
}
