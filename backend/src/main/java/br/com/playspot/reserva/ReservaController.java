package br.com.playspot.reserva;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ReservaRequest pedido) {
        return reservaService.criar(usuarioId(jwt), pedido);
    }

    @GetMapping("/minhas")
    public List<ReservaResponse> minhas(@AuthenticationPrincipal Jwt jwt) {
        return reservaService.minhas(usuarioId(jwt));
    }

    @PostMapping("/{id}/cancelamento")
    public ReservaResponse cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return reservaService.cancelar(usuarioId(jwt), id);
    }

    private Long usuarioId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
