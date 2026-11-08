package br.com.playspot.reserva;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.playspot.comum.NaoEncontradoException;
import br.com.playspot.comum.RegraDeNegocioException;
import br.com.playspot.empresa.Empresa;
import br.com.playspot.empresa.EmpresaRepository;
import br.com.playspot.horario.HorarioDisponivelResponse;
import br.com.playspot.horario.HorarioService;
import br.com.playspot.quadra.Quadra;
import br.com.playspot.quadra.QuadraRepository;

@Service
public class ReservaService {

    private static final String HORARIO_INDISPONIVEL = "Este horário não está mais disponível.";

    private final ReservaRepository reservas;
    private final QuadraRepository quadras;
    private final EmpresaRepository empresas;
    private final HorarioService horarioService;
    private final PagamentoRepository pagamentos;

    public ReservaService(
            ReservaRepository reservas,
            QuadraRepository quadras,
            EmpresaRepository empresas,
            HorarioService horarioService,
            PagamentoRepository pagamentos) {
        this.pagamentos = pagamentos;
        this.reservas = reservas;
        this.quadras = quadras;
        this.empresas = empresas;
        this.horarioService = horarioService;
    }

    /**
     * Sem @Transactional de propósito: se duas pessoas pedirem o mesmo horário ao mesmo tempo, as duas passam pela
     * consulta, mas o banco aceita só uma. A recusa precisa ser capturada aqui, fora de uma transação já marcada
     * para desfazer.
     */
    public ReservaResponse criar(Long usuarioId, ReservaRequest pedido) {
        Quadra quadra = quadras.findById(pedido.quadraId())
                .orElseThrow(() -> new NaoEncontradoException("Quadra não encontrada."));
        Empresa empresa = empresas.findById(quadra.getEmpresaId())
                .orElseThrow(() -> new NaoEncontradoException("Quadra não encontrada."));

        LocalDate data = pedido.inicio().atZone(ZoneId.of(empresa.getFuso())).toLocalDate();
        HorarioDisponivelResponse horario = horarioService.doDia(quadra.getId(), data).stream()
                .filter(h -> h.inicio().equals(pedido.inicio()) && h.livre())
                .findFirst()
                .orElseThrow(() -> new RegraDeNegocioException(HORARIO_INDISPONIVEL));

        try {
            Reserva reserva = reservas.saveAndFlush(
                    Reserva.doJogador(usuarioId, quadra.getId(), horario.inicio(), horario.fim(), horario.preco()));
            return resposta(reserva, quadra, empresa, false);
        } catch (DataIntegrityViolationException e) {
            throw new RegraDeNegocioException(HORARIO_INDISPONIVEL);
        }
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> minhas(Long usuarioId) {
        List<Reserva> lista = reservas.findByUsuarioIdOrderByInicioDesc(usuarioId);
        Map<Long, Quadra> quadrasPorId = quadras.findAllById(lista.stream().map(Reserva::getQuadraId).toList())
                .stream().collect(Collectors.toMap(Quadra::getId, Function.identity()));
        Map<Long, Empresa> empresasPorId = empresas
                .findAllById(quadrasPorId.values().stream().map(Quadra::getEmpresaId).toList())
                .stream().collect(Collectors.toMap(Empresa::getId, Function.identity()));

        Set<Long> pagas = pagamentos
                .findByReservaIdInAndStatus(lista.stream().map(Reserva::getId).toList(), StatusPagamento.PAGO)
                .stream().map(Pagamento::getReservaId).collect(Collectors.toSet());

        return lista.stream().map(reserva -> {
            Quadra quadra = quadrasPorId.get(reserva.getQuadraId());
            return resposta(reserva, quadra, empresasPorId.get(quadra.getEmpresaId()), pagas.contains(reserva.getId()));
        }).toList();
    }

    @Transactional
    public ReservaResponse cancelar(Long usuarioId, Long reservaId) {
        Reserva reserva = reservas.findByIdAndUsuarioId(reservaId, usuarioId)
                .orElseThrow(() -> new NaoEncontradoException("Reserva não encontrada."));
        if (pagamentos.findFirstByReservaIdAndStatus(reservaId, StatusPagamento.PAGO).isPresent()) {
            throw new RegraDeNegocioException("Esta reserva já foi paga. Fale com o local para cancelar.");
        }
        Quadra quadra = quadras.findById(reserva.getQuadraId()).orElseThrow();
        Empresa empresa = empresas.findById(quadra.getEmpresaId()).orElseThrow();

        if (reserva.getStatus() != StatusReserva.CONFIRMADA) {
            throw new RegraDeNegocioException("Esta reserva não pode mais ser cancelada.");
        }
        if (Instant.now().isAfter(limiteDeCancelamento(reserva, empresa))) {
            throw new RegraDeNegocioException("O prazo para cancelar passou. Fale com o local.");
        }
        reserva.cancelar();
        return resposta(reserva, quadra, empresa, false);
    }

    private Instant limiteDeCancelamento(Reserva reserva, Empresa empresa) {
        return reserva.getInicio().minus(Duration.ofHours(empresa.getPrazoCancelamentoHoras()));
    }

    private ReservaResponse resposta(Reserva reserva, Quadra quadra, Empresa empresa, boolean paga) {
        Instant limite = limiteDeCancelamento(reserva, empresa);
        return new ReservaResponse(
                reserva.getId(),
                empresa.getId(),
                empresa.getNome(),
                empresa.getEndereco(),
                empresa.getTelefone(),
                quadra.getNome(),
                reserva.getInicio(),
                reserva.getFim(),
                empresa.getFuso(),
                reserva.getPrecoTotal(),
                reserva.getStatus(),
                limite,
                reserva.getStatus() == StatusReserva.CONFIRMADA && !paga && !Instant.now().isAfter(limite));
    }
}
