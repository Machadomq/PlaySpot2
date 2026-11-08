package br.com.playspot.auth;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.playspot.usuario.EmailJaCadastradoException;
import br.com.playspot.usuario.Papel;
import br.com.playspot.usuario.Usuario;
import br.com.playspot.usuario.UsuarioRepository;

@Service
public class CadastroService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public CadastroService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario cadastrar(CadastroRequest pedido) {
        String email = pedido.email().trim().toLowerCase(Locale.ROOT);

        if (usuarios.existsByEmail(email)) {
            throw new EmailJaCadastradoException();
        }

        Usuario usuario = new Usuario(
                pedido.nome().trim(),
                email,
                passwordEncoder.encode(pedido.senha()),
                pedido.telefone(),
                Papel.valueOf(pedido.papel().name()));

        return usuarios.save(usuario);
    }
}
