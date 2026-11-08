package br.com.playspot.reserva;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByUsuarioIdOrderByInicioDesc(Long usuarioId);

    Optional<Reserva> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<Reserva> findByQuadraIdAndStatusNotAndInicioLessThanAndFimGreaterThan(
            Long quadraId, StatusReserva status, Instant ate, Instant desde);
}
