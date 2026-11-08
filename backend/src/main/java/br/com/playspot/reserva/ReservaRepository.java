package br.com.playspot.reserva;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByQuadraIdAndStatusNotAndInicioLessThanAndFimGreaterThan(
            Long quadraId, StatusReserva status, Instant ate, Instant desde);
}
