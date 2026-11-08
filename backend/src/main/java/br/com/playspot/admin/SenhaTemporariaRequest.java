package br.com.playspot.admin;

import jakarta.validation.constraints.NotBlank;

public record SenhaTemporariaRequest(@NotBlank(message = "Informe o e-mail.") String email) {
}
