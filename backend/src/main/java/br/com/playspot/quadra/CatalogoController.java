package br.com.playspot.quadra;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rotas públicas: lista de esportes e imagens das quadras. */
@RestController
@RequestMapping("/api")
public class CatalogoController {

    private final EsporteRepository esportes;
    private final QuadraService quadraService;

    public CatalogoController(EsporteRepository esportes, QuadraService quadraService) {
        this.esportes = esportes;
        this.quadraService = quadraService;
    }

    @GetMapping("/esportes")
    public List<EsporteResponse> esportes() {
        return esportes.findAllByOrderByNomeAsc().stream().map(EsporteResponse::de).toList();
    }

    @GetMapping("/fotos/{id}")
    public ResponseEntity<byte[]> foto(@PathVariable Long id) {
        QuadraService.FotoConteudo foto = quadraService.lerFoto(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.tipoConteudo()))
                .body(foto.bytes());
    }
}
