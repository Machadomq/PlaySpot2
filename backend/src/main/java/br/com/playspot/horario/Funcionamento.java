package br.com.playspot.horario;

import java.math.BigDecimal;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Faixa de horário com preço. hora_fim 00:00 significa "até a meia-noite". */
@Entity
@Table(name = "funcionamento")
public class Funcionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quadra_id", nullable = false, updatable = false)
    private Long quadraId;

    @Column(name = "dia_semana", nullable = false)
    private short diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    @Column(name = "preco_hora", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoHora;

    protected Funcionamento() {
        // exigido pelo JPA
    }

    public Funcionamento(Long quadraId, short diaSemana, LocalTime horaInicio, LocalTime horaFim, BigDecimal precoHora) {
        this.quadraId = quadraId;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.precoHora = precoHora;
    }

    public short getDiaSemana() {
        return diaSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFim() {
        return horaFim;
    }

    public BigDecimal getPrecoHora() {
        return precoHora;
    }
}
