package br.com.playspot.reserva;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByUsuarioIdOrderByInicioDesc(Long usuarioId);

    Optional<Reserva> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<Reserva> findByQuadraIdAndStatusNotAndInicioLessThanAndFimGreaterThan(
            Long quadraId, StatusReserva status, Instant ate, Instant desde);

    List<Reserva> findByQuadraIdInAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
            Collection<Long> quadraIds, Instant desde, Instant ate);

    /** Trava a linha até o fim da transação: dois cliques em "pago" ao mesmo tempo são atendidos um de cada vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> travarPorId(Long id);
}
