package br.com.playspot.empresa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    Optional<Empresa> findByDonoId(Long donoId);

    List<Empresa> findByAtivaTrueAndAprovadaTrueOrderByNomeAsc();

    List<Empresa> findAllByOrderByAprovadaAscNomeAsc();

    long countByAprovada(boolean aprovada);
}
