package br.com.playspot.reserva;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    List<Pagamento> findByReservaIdInAndStatus(Collection<Long> reservaIds, StatusPagamento status);

    Optional<Pagamento> findFirstByReservaIdAndStatus(Long reservaId, StatusPagamento status);
}
