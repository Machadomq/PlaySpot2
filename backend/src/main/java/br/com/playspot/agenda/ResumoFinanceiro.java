package br.com.playspot.agenda;

import java.math.BigDecimal;
import java.util.Map;

import br.com.playspot.reserva.MetodoPagamento;

/** Valores do período, considerando a data do jogo e não a data do pagamento. */
public record ResumoFinanceiro(
        BigDecimal recebido,
        Map<MetodoPagamento, BigDecimal> recebidoPorMetodo,
        BigDecimal aReceber,
        BigDecimal perdidoEmFaltas,
        int reservas,
        int faltas,
        int canceladas) {
}
