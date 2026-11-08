package br.com.playspot.horario;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.playspot.comum.DadosInvalidosException;
import br.com.playspot.comum.NaoEncontradoException;
import br.com.playspot.empresa.Empresa;
import br.com.playspot.empresa.EmpresaRepository;
import br.com.playspot.quadra.Quadra;
import br.com.playspot.quadra.QuadraRepository;
import br.com.playspot.quadra.QuadraService;
import br.com.playspot.reserva.Reserva;
import br.com.playspot.reserva.ReservaRepository;
import br.com.playspot.reserva.StatusReserva;

@Service
public class HorarioService {

    private static final List<String> NOMES_DOS_DIAS =
            List.of("domingo", "segunda", "terça", "quarta", "quinta", "sexta", "sábado");

    private final FuncionamentoRepository faixas;
    private final QuadraService quadraService;
    private final QuadraRepository quadras;
    private final EmpresaRepository empresas;
    private final ReservaRepository reservas;

    public HorarioService(
            FuncionamentoRepository faixas,
            QuadraService quadraService,
            QuadraRepository quadras,
            EmpresaRepository empresas,
            ReservaRepository reservas) {
        this.faixas = faixas;
        this.quadraService = quadraService;
        this.quadras = quadras;
        this.empresas = empresas;
        this.reservas = reservas;
    }

    @Transactional(readOnly = true)
    public List<FaixaResponse> listar(Long donoId, Long quadraId) {
        Quadra quadra = quadraService.quadraDoDono(donoId, quadraId);
        return faixas.findByQuadraIdOrderByDiaSemanaAscHoraInicioAsc(quadra.getId()).stream()
                .map(FaixaResponse::de)
                .toList();
    }

    @Transactional
    public List<FaixaResponse> substituir(Long donoId, Long quadraId, HorariosRequest dados) {
        Quadra quadra = quadraService.quadraDoDono(donoId, quadraId);
        validar(dados.faixas());

        faixas.deleteByQuadraId(quadra.getId());
        // a restrição do banco é verificada a cada comando: as faixas antigas precisam sair antes das novas entrarem
        faixas.flush();

        faixas.saveAll(dados.faixas().stream()
                .map(f -> new Funcionamento(quadra.getId(), f.diaSemana(), f.horaInicio(), f.horaFim(), f.precoHora()))
                .toList());
        faixas.flush();
        return faixas.findByQuadraIdOrderByDiaSemanaAscHoraInicioAsc(quadra.getId()).stream()
                .map(FaixaResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HorarioDisponivelResponse> doDia(Long quadraId, LocalDate data) {
        Quadra quadra = quadras.findById(quadraId).filter(Quadra::isAtiva)
                .orElseThrow(() -> new NaoEncontradoException("Quadra não encontrada."));
        Empresa empresa = empresas.findById(quadra.getEmpresaId()).filter(Empresa::isAtiva)
                .orElseThrow(() -> new NaoEncontradoException("Quadra não encontrada."));
        ZoneId fuso = ZoneId.of(empresa.getFuso());

        short diaSemana = (short) (data.getDayOfWeek().getValue() % 7);
        List<GeradorDeHorarios.Faixa> faixasDoDia = faixas.findByQuadraIdAndDiaSemana(quadraId, diaSemana).stream()
                .map(f -> new GeradorDeHorarios.Faixa(
                        GeradorDeHorarios.minutosInicio(f.getHoraInicio()),
                        GeradorDeHorarios.minutosFim(f.getHoraFim()),
                        f.getPrecoHora()))
                .toList();

        List<GeradorDeHorarios.Slot> slots = GeradorDeHorarios.gerar(faixasDoDia, quadra.getDuracaoSlotMin());
        if (slots.isEmpty()) {
            return List.of();
        }

        List<Reserva> ocupadas = reservas.findByQuadraIdAndStatusNotAndInicioLessThanAndFimGreaterThan(
                quadraId,
                StatusReserva.CANCELADA,
                instante(data, slots.getLast().fim(), fuso),
                instante(data, slots.getFirst().inicio(), fuso));

        Instant agora = Instant.now();
        return slots.stream().map(slot -> {
            Instant inicio = instante(data, slot.inicio(), fuso);
            Instant fim = instante(data, slot.fim(), fuso);
            boolean ocupado = ocupadas.stream()
                    .anyMatch(r -> inicio.isBefore(r.getFim()) && fim.isAfter(r.getInicio()));
            return new HorarioDisponivelResponse(
                    inicio,
                    fim,
                    horaDoDia(slot.inicio()),
                    horaDoDia(slot.fim()),
                    slot.preco(),
                    !ocupado && inicio.isAfter(agora));
        }).toList();
    }

    private void validar(List<HorariosRequest.Faixa> lista) {
        for (HorariosRequest.Faixa faixa : lista) {
            if (minutosFim(faixa) <= minutosInicio(faixa)) {
                throw new DadosInvalidosException(
                        "Na " + NOMES_DOS_DIAS.get(faixa.diaSemana()) + ", o fim da faixa deve ser depois do início.");
            }
        }
        lista.stream().collect(Collectors.groupingBy(HorariosRequest.Faixa::diaSemana)).forEach((dia, doDia) -> {
            List<HorariosRequest.Faixa> ordenadas =
                    doDia.stream().sorted(Comparator.comparingInt(this::minutosInicio)).toList();
            for (int i = 1; i < ordenadas.size(); i++) {
                if (minutosInicio(ordenadas.get(i)) < minutosFim(ordenadas.get(i - 1))) {
                    throw new DadosInvalidosException("Há faixas sobrepostas na " + NOMES_DOS_DIAS.get(dia) + ".");
                }
            }
        });
    }

    private int minutosInicio(HorariosRequest.Faixa faixa) {
        return GeradorDeHorarios.minutosInicio(faixa.horaInicio());
    }

    private int minutosFim(HorariosRequest.Faixa faixa) {
        return GeradorDeHorarios.minutosFim(faixa.horaFim());
    }

    private Instant instante(LocalDate data, int minutos, ZoneId fuso) {
        return LocalDateTime.of(data, LocalTime.MIDNIGHT).plusMinutes(minutos).atZone(fuso).toInstant();
    }

    private LocalTime horaDoDia(int minutos) {
        return LocalTime.MIDNIGHT.plusMinutes(minutos);
    }
}
