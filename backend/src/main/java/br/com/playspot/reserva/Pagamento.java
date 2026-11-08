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
@Table(name = "pagamento")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reserva_id", nullable = false, updatable = false)
    private Long reservaId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoPagamento metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPagamento status;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected Pagamento() {
        // exigido pelo JPA
    }

    public static Pagamento recebido(Long reservaId, BigDecimal valor, MetodoPagamento metodo) {
        Pagamento pagamento = new Pagamento();
        pagamento.reservaId = reservaId;
        pagamento.valor = valor;
        pagamento.metodo = metodo;
        pagamento.status = StatusPagamento.PAGO;
        return pagamento;
    }

    public void estornar() {
        this.status = StatusPagamento.ESTORNADO;
    }

    public Long getReservaId() {
        return reservaId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public MetodoPagamento getMetodo() {
        return metodo;
    }
}
