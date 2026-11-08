package br.com.playspot.empresa;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmpresaRequest(
        @NotBlank(message = "Informe o nome da empresa.")
        @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres.")
        String nome,

        @Size(max = 20, message = "O documento pode ter no máximo 20 caracteres.")
        String documento,

        @Size(max = 20, message = "O telefone pode ter no máximo 20 caracteres.")
        String telefone,

        @NotBlank(message = "Informe o endereço.")
        @Size(max = 200, message = "O endereço pode ter no máximo 200 caracteres.")
        String endereco,

        @NotBlank(message = "Informe a cidade.")
        @Size(max = 100, message = "A cidade pode ter no máximo 100 caracteres.")
        String cidade,

        @NotBlank(message = "Informe a UF.")
        @Pattern(regexp = "[A-Z]{2}", message = "Informe a UF com duas letras maiúsculas.")
        String uf,

        @Min(value = 0, message = "O prazo de cancelamento não pode ser negativo.")
        @Max(value = 720, message = "O prazo de cancelamento pode ser de no máximo 720 horas.")
        int prazoCancelamentoHoras,

        @NotNull(message = "Marque a localização da empresa no mapa.")
        @DecimalMin(value = "-90", message = "Localização inválida.")
        @DecimalMax(value = "90", message = "Localização inválida.")
        BigDecimal latitude,

        @NotNull(message = "Marque a localização da empresa no mapa.")
        @DecimalMin(value = "-180", message = "Localização inválida.")
        @DecimalMax(value = "180", message = "Localização inválida.")
        BigDecimal longitude) {
}
