package br.com.playspot.agenda;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BloqueioRequest(
        @NotNull(message = "Escolha a quadra.") Long quadraId,
        @NotNull(message = "Escolha o horário.") Instant inicio,
        @NotNull(message = "Escolha o horário.") Instant fim,

        @NotBlank(message = "Informe o motivo do bloqueio.")
        @Size(max = 120, message = "O motivo pode ter no máximo 120 caracteres.")
        String motivo) {
}
