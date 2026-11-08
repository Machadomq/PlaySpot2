package br.com.playspot.horario;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class HorarioController {

    private final HorarioService horarioService;

    public HorarioController(HorarioService horarioService) {
        this.horarioService = horarioService;
    }

    @GetMapping("/dono/quadras/{id}/horarios")
    public List<FaixaResponse> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return horarioService.listar(Long.valueOf(jwt.getSubject()), id);
    }

    @PutMapping("/dono/quadras/{id}/horarios")
    public List<FaixaResponse> substituir(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody HorariosRequest dados) {
        return horarioService.substituir(Long.valueOf(jwt.getSubject()), id, dados);
    }

    /** Rota pública: horários de uma quadra numa data, no calendário da empresa. */
    @GetMapping("/quadras/{id}/horarios")
    public List<HorarioDisponivelResponse> doDia(
            @PathVariable Long id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return horarioService.doDia(id, data);
    }
}
