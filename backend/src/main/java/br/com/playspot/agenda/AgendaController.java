package br.com.playspot.agenda;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/dono")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @GetMapping("/agenda")
    public List<AgendaItem> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return agendaService.listar(donoId(jwt), de, ate);
    }

    @GetMapping("/financeiro")
    public ResumoFinanceiro financeiro(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return agendaService.resumo(donoId(jwt), de, ate);
    }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public AgendaItem criarManual(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ReservaManualRequest pedido) {
        return agendaService.criarManual(donoId(jwt), pedido);
    }

    @PostMapping("/bloqueios")
    @ResponseStatus(HttpStatus.CREATED)
    public AgendaItem bloquear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BloqueioRequest pedido) {
        return agendaService.bloquear(donoId(jwt), pedido);
    }

    @PostMapping("/reservas/{id}/pagamento")
    public AgendaItem registrarPagamento(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody PagamentoRequest pedido) {
        return agendaService.registrarPagamento(donoId(jwt), id, pedido.metodo());
    }

    @PostMapping("/reservas/{id}/estorno")
    public AgendaItem estornar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return agendaService.estornarPagamento(donoId(jwt), id);
    }

    @PostMapping("/reservas/{id}/falta")
    public AgendaItem marcarFalta(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return agendaService.marcarFalta(donoId(jwt), id);
    }

    @PostMapping("/reservas/{id}/falta/desfazer")
    public AgendaItem desfazerFalta(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return agendaService.desfazerFalta(donoId(jwt), id);
    }

    @PostMapping("/reservas/{id}/cancelamento")
    public AgendaItem cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return agendaService.cancelar(donoId(jwt), id);
    }

    private Long donoId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
