package br.com.playspot.reserva;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

/** O preço e o fim não vêm do cliente: são calculados no servidor. */
public record ReservaRequest(
        @NotNull(message = "Escolha a quadra.") Long quadraId,
        @NotNull(message = "Escolha o horário.") Instant inicio) {
}
