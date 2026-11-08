package br.com.playspot.quadra;

import java.util.Comparator;
import java.util.List;

public record QuadraResponse(
        Long id,
        String nome,
        String descricao,
        int duracaoSlotMin,
        boolean ativa,
        List<EsporteResponse> esportes,
        List<Long> fotoIds) {

    public static QuadraResponse de(Quadra quadra, List<Long> fotoIds) {
        List<EsporteResponse> esportes = quadra.getEsportes().stream()
                .map(EsporteResponse::de)
                .sorted(Comparator.comparing(EsporteResponse::nome))
                .toList();
        return new QuadraResponse(
                quadra.getId(),
                quadra.getNome(),
                quadra.getDescricao(),
                quadra.getDuracaoSlotMin(),
                quadra.isAtiva(),
                esportes,
                fotoIds);
    }
}
