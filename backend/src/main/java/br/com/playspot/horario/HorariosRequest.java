package br.com.playspot.horario;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A semana inteira da quadra: o que não vier na lista deixa de existir. */
public record HorariosRequest(
        @NotNull(message = "Informe as faixas de horário.")
        @Size(max = 70, message = "São permitidas no máximo 70 faixas por semana.")
        List<@Valid Faixa> faixas) {

    public record Faixa(
            @NotNull(message = "Informe o dia da semana.")
            @Min(value = 0, message = "Dia da semana inválido.")
            @Max(value = 6, message = "Dia da semana inválido.")
            Short diaSemana,

            @NotNull(message = "Informe o início de todas as faixas.")
            LocalTime horaInicio,

            @NotNull(message = "Informe o fim de todas as faixas.")
            LocalTime horaFim,

            @NotNull(message = "Informe o preço de todas as faixas.")
            @DecimalMin(value = "0", message = "O preço não pode ser negativo.")
            @Digits(integer = 8, fraction = 2, message = "Preço inválido.")
            BigDecimal precoHora) {
    }
}
