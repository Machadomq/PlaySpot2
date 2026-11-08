package br.com.playspot.quadra;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuadraFotoRepository extends JpaRepository<QuadraFoto, Long> {

    List<QuadraFoto> findByQuadraIdInOrderByIdAsc(Collection<Long> quadraIds);

    long countByQuadraId(Long quadraId);

    Optional<QuadraFoto> findByIdAndQuadraId(Long id, Long quadraId);
}
