package br.com.playspot.busca;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BuscaController {

    private final BuscaService buscaService;

    public BuscaController(BuscaService buscaService) {
        this.buscaService = buscaService;
    }

    @GetMapping("/busca")
    public List<ResultadoBusca> buscar(
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) Short esporteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "30") int raioKm) {
        int raioPermitido = Math.clamp(raioKm, 1, 100);
        return buscaService.buscar(
                new BuscaService.Filtros(cidade, esporteId, data, latitude, longitude, raioPermitido));
    }

    @GetMapping("/empresas/{id}")
    public EmpresaPublicaResponse empresa(@PathVariable Long id) {
        return buscaService.empresa(id);
    }
}
