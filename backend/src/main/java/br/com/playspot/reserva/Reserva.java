package br.com.playspot.reserva;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Por enquanto só o necessário para calcular os horários livres. */
@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    private Long id;

    @Column(name = "quadra_id", nullable = false)
    private Long quadraId;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusReserva status;

    protected Reserva() {
        // exigido pelo JPA
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFim() {
        return fim;
    }
}
