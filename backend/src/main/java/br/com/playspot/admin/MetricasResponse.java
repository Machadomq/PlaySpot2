package br.com.playspot.admin;

/** Reservas contadas pela data do jogo, nos últimos 30 dias. Horários bloqueados não entram. */
public record MetricasResponse(
        long jogadores,
        long donos,
        long empresasAprovadas,
        long empresasPendentes,
        long reservasConfirmadas,
        long faltas,
        long canceladas) {
}
