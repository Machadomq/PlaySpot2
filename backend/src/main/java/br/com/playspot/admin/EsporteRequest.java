package br.com.playspot.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EsporteRequest(
        @NotBlank(message = "Informe o nome do esporte.")
        @Size(max = 60, message = "O nome pode ter no máximo 60 caracteres.")
        String nome) {
}
