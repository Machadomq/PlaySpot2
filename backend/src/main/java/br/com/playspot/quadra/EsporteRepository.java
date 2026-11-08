package br.com.playspot.quadra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EsporteRepository extends JpaRepository<Esporte, Short> {

    List<Esporte> findAllByOrderByNomeAsc();

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Short id);
}
