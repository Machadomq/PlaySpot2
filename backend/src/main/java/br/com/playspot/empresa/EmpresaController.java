package br.com.playspot.empresa;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/dono/empresa")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping
    public EmpresaResponse minha(@AuthenticationPrincipal Jwt jwt) {
        return EmpresaResponse.de(empresaService.doDono(Long.valueOf(jwt.getSubject())));
    }

    @PutMapping
    public EmpresaResponse salvar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody EmpresaRequest dados) {
        return EmpresaResponse.de(empresaService.salvar(Long.valueOf(jwt.getSubject()), dados));
    }
}
