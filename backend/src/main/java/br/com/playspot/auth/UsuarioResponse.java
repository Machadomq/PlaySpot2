package br.com.playspot.auth;

import br.com.playspot.usuario.Papel;
import br.com.playspot.usuario.Usuario;

/** Dados do usuário que a API devolve. Não inclui a senha. */
public record UsuarioResponse(Long id, String nome, String email, String telefone, Papel papel) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getTelefone(), usuario.getPapel());
    }
}
