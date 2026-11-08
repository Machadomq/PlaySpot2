package br.com.playspot.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dados que o cliente envia para se cadastrar. */
public record CadastroRequest(
        @NotBlank(message = "Informe o nome.")
        @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres.")
        String nome,

        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 254, message = "O e-mail pode ter no máximo 254 caracteres.")
        String email,

        @NotBlank(message = "Informe a senha.")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        String senha,

        @Size(max = 20, message = "O telefone pode ter no máximo 20 caracteres.")
        String telefone,

        @NotNull(message = "Escolha o tipo de conta.")
        PapelCadastro papel) {

    /** Papéis que podem ser escolhidos no cadastro; ADMIN fica de fora de propósito. */
    public enum PapelCadastro {
        JOGADOR,
        DONO
    }
}
