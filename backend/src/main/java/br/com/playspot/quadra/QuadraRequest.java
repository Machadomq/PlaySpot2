package br.com.playspot.quadra;

import java.util.Set;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record QuadraRequest(
        @NotBlank(message = "Informe o nome da quadra.")
        @Size(max = 80, message = "O nome pode ter no máximo 80 caracteres.")
        String nome,

        @Size(max = 1000, message = "A descrição pode ter no máximo 1000 caracteres.")
        String descricao,

        @Min(value = 30, message = "A duração mínima do horário é de 30 minutos.")
        @Max(value = 240, message = "A duração máxima do horário é de 240 minutos.")
        int duracaoSlotMin,

        boolean ativa,

        @NotEmpty(message = "Escolha pelo menos um esporte.")
        Set<Short> esporteIds) {
}
