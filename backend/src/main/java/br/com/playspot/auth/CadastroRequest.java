package br.com.playspot.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dados que o cliente envia para se cadastrar. */
public record CadastroRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String senha,
        @Size(max = 20) String telefone,
        @NotNull PapelCadastro papel) {

    /** Papéis que podem ser escolhidos no cadastro; ADMIN fica de fora de propósito. */
    public enum PapelCadastro {
        JOGADOR,
        DONO
    }
}
