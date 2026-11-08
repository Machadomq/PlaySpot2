package br.com.playspot.reserva;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "quadra_id", nullable = false, updatable = false)
    private Long quadraId;

    @Column(name = "cliente_nome")
    private String clienteNome;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fim;

    /** Copiado no momento da reserva: mudanças futuras de preço não alteram reservas já feitas. */
    @Column(name = "preco_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusReserva status;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm = Instant.now();

    protected Reserva() {
        // exigido pelo JPA
    }

    public static Reserva doJogador(Long usuarioId, Long quadraId, Instant inicio, Instant fim, BigDecimal preco) {
        Reserva reserva = new Reserva();
        reserva.usuarioId = usuarioId;
        reserva.quadraId = quadraId;
        reserva.inicio = inicio;
        reserva.fim = fim;
        reserva.precoTotal = preco;
        reserva.status = StatusReserva.CONFIRMADA;
        return reserva;
    }

    public void cancelar() {
        this.status = StatusReserva.CANCELADA;
    }

    public Long getId() {
        return id;
    }

    public Long getQuadraId() {
        return quadraId;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFim() {
        return fim;
    }

    public BigDecimal getPrecoTotal() {
        return precoTotal;
    }

    public StatusReserva getStatus() {
        return status;
    }
}
