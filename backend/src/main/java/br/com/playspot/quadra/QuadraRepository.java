package br.com.playspot.quadra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuadraRepository extends JpaRepository<Quadra, Long> {

    List<Quadra> findByEmpresaIdOrderByNomeAsc(Long empresaId);

    List<Quadra> findByEmpresaIdAndAtivaTrueOrderByNomeAsc(Long empresaId);

    Optional<Quadra> findByIdAndEmpresaId(Long id, Long empresaId);
}
