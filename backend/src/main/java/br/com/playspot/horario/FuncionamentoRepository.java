package br.com.playspot.horario;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FuncionamentoRepository extends JpaRepository<Funcionamento, Long> {

    List<Funcionamento> findByQuadraIdOrderByDiaSemanaAscHoraInicioAsc(Long quadraId);

    List<Funcionamento> findByQuadraIdAndDiaSemana(Long quadraId, short diaSemana);

    void deleteByQuadraId(Long quadraId);
}
