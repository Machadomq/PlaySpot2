package br.com.playspot.auth;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.playspot.comum.DadosInvalidosException;
import br.com.playspot.usuario.CredenciaisInvalidasException;
import br.com.playspot.usuario.Usuario;
import br.com.playspot.usuario.UsuarioRepository;

@Service
public class LoginService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    /** Hash usado quando o e-mail não existe, para o tempo de resposta não revelar se o e-mail está cadastrado. */
    private final String hashFalso;

    public LoginService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.hashFalso = passwordEncoder.encode("senha-que-ninguem-usa");
    }

    @Transactional
    public void trocarSenha(Long usuarioId, TrocarSenhaRequest pedido) {
        Usuario usuario = usuarios.findById(usuarioId).orElseThrow(CredenciaisInvalidasException::new);
        if (!passwordEncoder.matches(pedido.senhaAtual(), usuario.getSenhaHash())) {
            throw new DadosInvalidosException("A senha atual está incorreta.");
        }
        usuario.trocarSenha(passwordEncoder.encode(pedido.novaSenha()));
    }

    public LoginResponse entrar(LoginRequest pedido) {
        String email = pedido.email().trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarios.findByEmail(email).orElse(null);

        String hashParaConferir = usuario != null ? usuario.getSenhaHash() : hashFalso;
        boolean senhaConfere = passwordEncoder.matches(pedido.senha(), hashParaConferir);

        if (usuario == null || !senhaConfere) {
            throw new CredenciaisInvalidasException();
        }

        return new LoginResponse(
                tokenService.gerar(usuario),
                "Bearer",
                tokenService.validade().toSeconds(),
                UsuarioResponse.de(usuario));
    }
}
