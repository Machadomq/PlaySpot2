package br.com.playspot.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import br.com.playspot.usuario.CredenciaisInvalidasException;
import br.com.playspot.usuario.UsuarioRepository;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class LoginController {

    private final LoginService loginService;
    private final UsuarioRepository usuarios;

    public LoginController(LoginService loginService, UsuarioRepository usuarios) {
        this.loginService = loginService;
        this.usuarios = usuarios;
    }

    @PostMapping("/auth/login")
    public LoginResponse entrar(@Valid @RequestBody LoginRequest pedido) {
        return loginService.entrar(pedido);
    }

    /** Devolve o usuário dono do token enviado; serve para o front saber "quem sou eu". */
    @GetMapping("/me")
    public UsuarioResponse eu(@AuthenticationPrincipal Jwt jwt) {
        return usuarios.findById(Long.valueOf(jwt.getSubject()))
                .map(UsuarioResponse::de)
                .orElseThrow(CredenciaisInvalidasException::new);
    }
}
