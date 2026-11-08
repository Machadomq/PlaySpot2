package br.com.playspot.busca;

import java.math.BigDecimal;
import java.util.List;

/** Empresa encontrada na busca. distanciaKm é nula em buscas só por cidade. */
public record ResultadoBusca(
        Long id,
        String nome,
        String endereco,
        String cidade,
        String uf,
        BigDecimal latitude,
        BigDecimal longitude,
        Double distanciaKm,
        List<String> esportes,
        Long fotoId,
        int horariosLivres,
        BigDecimal precoAPartirDe) {
}
