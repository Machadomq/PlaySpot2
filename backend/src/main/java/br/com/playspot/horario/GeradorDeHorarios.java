package br.com.playspot.horario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Transforma as faixas de um dia em horários reserváveis. Trabalha em minutos desde o início do dia. */
final class GeradorDeHorarios {

    static final int MINUTOS_NO_DIA = 24 * 60;

    record Faixa(int inicio, int fim, BigDecimal precoHora) {
    }

    record Slot(int inicio, int fim, BigDecimal preco) {
    }

    private GeradorDeHorarios() {
    }

    static int minutosInicio(LocalTime hora) {
        return hora.getHour() * 60 + hora.getMinute();
    }

    static int minutosFim(LocalTime hora) {
        return hora.equals(LocalTime.MIDNIGHT) ? MINUTOS_NO_DIA : minutosInicio(hora);
    }

    /** Faixas encostadas formam um bloco contínuo, então um horário pode começar numa faixa e terminar na seguinte. */
    static List<Slot> gerar(List<Faixa> faixas, int duracaoMinutos) {
        List<Faixa> ordenadas = faixas.stream().sorted(Comparator.comparingInt(Faixa::inicio)).toList();
        List<Slot> slots = new ArrayList<>();

        int i = 0;
        while (i < ordenadas.size()) {
            int inicioBloco = ordenadas.get(i).inicio();
            int fimBloco = ordenadas.get(i).fim();
            while (i + 1 < ordenadas.size() && ordenadas.get(i + 1).inicio() == fimBloco) {
                i++;
                fimBloco = ordenadas.get(i).fim();
            }
            for (int t = inicioBloco; t + duracaoMinutos <= fimBloco; t += duracaoMinutos) {
                slots.add(new Slot(t, t + duracaoMinutos, preco(ordenadas, t, t + duracaoMinutos)));
            }
            i++;
        }
        return slots;
    }

    /** Cada minuto custa o preço da faixa em que cai. */
    private static BigDecimal preco(List<Faixa> faixas, int inicio, int fim) {
        BigDecimal soma = BigDecimal.ZERO;
        for (Faixa faixa : faixas) {
            int minutos = Math.min(fim, faixa.fim()) - Math.max(inicio, faixa.inicio());
            if (minutos > 0) {
                soma = soma.add(faixa.precoHora().multiply(BigDecimal.valueOf(minutos)));
            }
        }
        return soma.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}
