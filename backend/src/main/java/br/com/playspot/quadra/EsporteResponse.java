package br.com.playspot.quadra;

public record EsporteResponse(Short id, String nome) {

    public static EsporteResponse de(Esporte esporte) {
        return new EsporteResponse(esporte.getId(), esporte.getNome());
    }
}
