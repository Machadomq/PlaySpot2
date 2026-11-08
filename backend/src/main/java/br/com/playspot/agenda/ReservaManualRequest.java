package br.com.playspot.agenda;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReservaManualRequest(
        @NotNull(message = "Escolha a quadra.") Long quadraId,
        @NotNull(message = "Escolha o horário.") Instant inicio,
        @NotNull(message = "Escolha o horário.") Instant fim,

        @NotBlank(message = "Informe o nome do cliente.")
        @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres.")
        String clienteNome,

        @NotNull(message = "Informe o valor.")
        @DecimalMin(value = "0", message = "O valor não pode ser negativo.")
        @Digits(integer = 8, fraction = 2, message = "Valor inválido.")
        BigDecimal preco) {
}
