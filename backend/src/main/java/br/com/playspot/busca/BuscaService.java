package br.com.playspot.busca;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.playspot.comum.DadosInvalidosException;
import br.com.playspot.comum.NaoEncontradoException;
import br.com.playspot.empresa.Empresa;
import br.com.playspot.empresa.EmpresaRepository;
import br.com.playspot.horario.HorarioDisponivelResponse;
import br.com.playspot.horario.HorarioService;
import br.com.playspot.quadra.EsporteResponse;
import br.com.playspot.quadra.QuadraResponse;
import br.com.playspot.quadra.QuadraService;

@Service
public class BuscaService {

    private static final int MAXIMO_DE_RESULTADOS = 30;
    private static final double RAIO_DA_TERRA_KM = 6371;

    private final EmpresaRepository empresas;
    private final QuadraService quadraService;
    private final HorarioService horarioService;

    public BuscaService(EmpresaRepository empresas, QuadraService quadraService, HorarioService horarioService) {
        this.empresas = empresas;
        this.quadraService = quadraService;
        this.horarioService = horarioService;
    }

    public record Filtros(String cidade, Short esporteId, LocalDate data, Double latitude, Double longitude, int raioKm) {

        boolean temCoordenadas() {
            return latitude != null;
        }

        boolean temCidade() {
            return cidade != null && !cidade.isBlank();
        }
    }

    private record Candidata(Empresa empresa, Double distanciaKm) {
    }

    @Transactional(readOnly = true)
    public List<ResultadoBusca> buscar(Filtros filtros) {
        if ((filtros.latitude() == null) != (filtros.longitude() == null)) {
            throw new DadosInvalidosException("Localização inválida.");
        }
        if (!filtros.temCoordenadas() && !filtros.temCidade()) {
            throw new DadosInvalidosException("Informe uma cidade ou use sua localização.");
        }

        List<Candidata> candidatas = empresas.findByAtivaTrueAndAprovadaTrueOrderByNomeAsc().stream()
                .filter(e -> !filtros.temCidade() || normalizar(e.getCidade()).equals(normalizar(filtros.cidade())))
                .filter(e -> !filtros.temCoordenadas() || e.getLatitude() != null)
                .map(e -> new Candidata(e, filtros.temCoordenadas() ? distanciaKm(e, filtros) : null))
                .filter(c -> c.distanciaKm() == null || c.distanciaKm() <= filtros.raioKm())
                .sorted(filtros.temCoordenadas() ? Comparator.comparing(Candidata::distanciaKm) : (a, b) -> 0)
                .toList();

        List<ResultadoBusca> resultados = new ArrayList<>();
        for (Candidata candidata : candidatas) {
            if (resultados.size() == MAXIMO_DE_RESULTADOS) {
                break;
            }
            resumir(candidata, filtros).ifPresent(resultados::add);
        }
        return resultados;
    }

    @Transactional(readOnly = true)
    public EmpresaPublicaResponse empresa(Long id) {
        Empresa empresa = empresas.findById(id).filter(Empresa::isAtiva).filter(Empresa::isAprovada)
                .orElseThrow(() -> new NaoEncontradoException("Empresa não encontrada."));
        return EmpresaPublicaResponse.de(empresa, quadraService.ativasDaEmpresa(id));
    }

    /** Resume a empresa considerando só as quadras do esporte pedido; sem horário livre na data, ela não aparece. */
    private Optional<ResultadoBusca> resumir(Candidata candidata, Filtros filtros) {
        Empresa empresa = candidata.empresa();
        List<QuadraResponse> quadras = quadraService.ativasDaEmpresa(empresa.getId()).stream()
                .filter(q -> filtros.esporteId() == null
                        || q.esportes().stream().anyMatch(e -> e.id().equals(filtros.esporteId())))
                .toList();

        int livres = 0;
        BigDecimal menorPreco = null;
        for (QuadraResponse quadra : quadras) {
            for (HorarioDisponivelResponse horario : horarioService.doDia(quadra.id(), filtros.data())) {
                if (horario.livre()) {
                    livres++;
                    menorPreco = menorPreco == null ? horario.preco() : menorPreco.min(horario.preco());
                }
            }
        }
        if (livres == 0) {
            return Optional.empty();
        }

        TreeSet<String> esportes = new TreeSet<>();
        quadras.forEach(q -> q.esportes().stream().map(EsporteResponse::nome).forEach(esportes::add));
        Long foto = quadras.stream().flatMap(q -> q.fotoIds().stream()).findFirst().orElse(null);

        return Optional.of(new ResultadoBusca(
                empresa.getId(),
                empresa.getNome(),
                empresa.getEndereco(),
                empresa.getCidade(),
                empresa.getUf(),
                empresa.getLatitude(),
                empresa.getLongitude(),
                candidata.distanciaKm(),
                List.copyOf(esportes),
                foto,
                livres,
                menorPreco));
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    /** Fórmula de haversine: distância em linha reta sobre a superfície da Terra. */
    private double distanciaKm(Empresa empresa, Filtros filtros) {
        double lat1 = Math.toRadians(filtros.latitude());
        double lat2 = Math.toRadians(empresa.getLatitude().doubleValue());
        double deltaLat = lat2 - lat1;
        double deltaLng = Math.toRadians(empresa.getLongitude().doubleValue() - filtros.longitude());
        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLng / 2), 2);
        return 2 * RAIO_DA_TERRA_KM * Math.asin(Math.sqrt(a));
    }
}
