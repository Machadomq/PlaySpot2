package br.com.playspot.quadra;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/dono/quadras")
public class QuadraController {

    private final QuadraService quadraService;

    public QuadraController(QuadraService quadraService) {
        this.quadraService = quadraService;
    }

    @GetMapping
    public List<QuadraResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return quadraService.listar(donoId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuadraResponse criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody QuadraRequest dados) {
        return quadraService.criar(donoId(jwt), dados);
    }

    @PutMapping("/{id}")
    public QuadraResponse atualizar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody QuadraRequest dados) {
        return quadraService.atualizar(donoId(jwt), id, dados);
    }

    @PostMapping("/{id}/fotos")
    public QuadraResponse adicionarFoto(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @RequestParam("arquivo") MultipartFile arquivo) {
        return quadraService.adicionarFoto(donoId(jwt), id, arquivo);
    }

    @DeleteMapping("/{id}/fotos/{fotoId}")
    public QuadraResponse removerFoto(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @PathVariable Long fotoId) {
        return quadraService.removerFoto(donoId(jwt), id, fotoId);
    }

    private Long donoId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
