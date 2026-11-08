package br.com.playspot.reserva;

import java.math.BigDecimal;
import java.time.Instant;

public record ReservaResponse(
        Long id,
        Long empresaId,
        String empresaNome,
        String endereco,
        String telefone,
        String quadraNome,
        Instant inicio,
        Instant fim,
        String fuso,
        BigDecimal precoTotal,
        StatusReserva status,
        Instant canceladaAte,
        boolean podeCancelar) {
}
