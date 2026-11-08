package br.com.playspot.agenda;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.playspot.comum.DadosInvalidosException;
import br.com.playspot.comum.NaoEncontradoException;
import br.com.playspot.comum.RegraDeNegocioException;
import br.com.playspot.empresa.Empresa;
import br.com.playspot.empresa.EmpresaRepository;
import br.com.playspot.quadra.Quadra;
import br.com.playspot.quadra.QuadraRepository;
import br.com.playspot.quadra.QuadraService;
import br.com.playspot.reserva.MetodoPagamento;
import br.com.playspot.reserva.Pagamento;
import br.com.playspot.reserva.PagamentoRepository;
import br.com.playspot.reserva.Reserva;
import br.com.playspot.reserva.ReservaRepository;
import br.com.playspot.reserva.StatusPagamento;
import br.com.playspot.reserva.StatusReserva;
import br.com.playspot.usuario.Usuario;
import br.com.playspot.usuario.UsuarioRepository;

@Service
public class AgendaService {

    private static final Duration DURACAO_MAXIMA = Duration.ofHours(24);
    private static final int MAXIMO_DE_DIAS_NA_AGENDA = 31;
    private static final int MAXIMO_DE_DIAS_NO_RESUMO = 366;

    private final ReservaRepository reservas;
    private final PagamentoRepository pagamentos;
    private final QuadraRepository quadras;
    private final QuadraService quadraService;
    private final EmpresaRepository empresas;
    private final UsuarioRepository usuarios;

    public AgendaService(
            ReservaRepository reservas,
            PagamentoRepository pagamentos,
            QuadraRepository quadras,
            QuadraService quadraService,
            EmpresaRepository empresas,
            UsuarioRepository usuarios) {
        this.reservas = reservas;
        this.pagamentos = pagamentos;
        this.quadras = quadras;
        this.quadraService = quadraService;
        this.empresas = empresas;
        this.usuarios = usuarios;
    }

    /** Reservas canceladas ficam fora: o horário delas volta a estar livre. */
    @Transactional(readOnly = true)
    public List<AgendaItem> listar(Long donoId, LocalDate de, LocalDate ate) {
        validarPeriodo(de, ate, MAXIMO_DE_DIAS_NA_AGENDA);
        return empresas.findByDonoId(donoId)
                .map(empresa -> itens(empresa, doPeriodo(empresa, de, ate).stream()
                        .filter(r -> r.getStatus() != StatusReserva.CANCELADA)
                        .toList()))
                .orElse(List.of());
    }

    /** Sem @Transactional: a recusa do banco por conflito de horário é capturada aqui (ver ReservaService.criar). */
    public AgendaItem criarManual(Long donoId, ReservaManualRequest pedido) {
        validarIntervalo(pedido.inicio(), pedido.fim());
        Quadra quadra = quadraService.quadraDoDono(donoId, pedido.quadraId());
        return salvar(donoId, Reserva.manual(
                quadra.getId(), pedido.clienteNome().trim(), pedido.inicio(), pedido.fim(), pedido.preco()));
    }

    public AgendaItem bloquear(Long donoId, BloqueioRequest pedido) {
        validarIntervalo(pedido.inicio(), pedido.fim());
        Quadra quadra = quadraService.quadraDoDono(donoId, pedido.quadraId());
        return salvar(donoId, Reserva.bloqueio(quadra.getId(), pedido.motivo().trim(), pedido.inicio(), pedido.fim()));
    }

    @Transactional
    public AgendaItem registrarPagamento(Long donoId, Long reservaId, MetodoPagamento metodo) {
        Reserva reserva = reservaDoDono(donoId, reservaId);
        if (reserva.getStatus() != StatusReserva.CONFIRMADA && reserva.getStatus() != StatusReserva.FALTOU) {
            throw new RegraDeNegocioException("Esta reserva não aceita pagamento.");
        }
        if (reserva.getPrecoTotal().signum() == 0) {
            throw new RegraDeNegocioException("Esta reserva não tem valor a receber.");
        }
        if (pagamentos.findFirstByReservaIdAndStatus(reservaId, StatusPagamento.PAGO).isPresent()) {
            throw new RegraDeNegocioException("O pagamento desta reserva já foi registrado.");
        }
        pagamentos.saveAndFlush(Pagamento.recebido(reservaId, reserva.getPrecoTotal(), metodo));
        return item(donoId, reserva);
    }

    @Transactional
    public AgendaItem estornarPagamento(Long donoId, Long reservaId) {
        Reserva reserva = reservaDoDono(donoId, reservaId);
        pagamentos.findFirstByReservaIdAndStatus(reservaId, StatusPagamento.PAGO)
                .orElseThrow(() -> new RegraDeNegocioException("Esta reserva não tem pagamento registrado."))
                .estornar();
        pagamentos.flush();
        return item(donoId, reserva);
    }

    @Transactional
    public AgendaItem marcarFalta(Long donoId, Long reservaId) {
        Reserva reserva = reservaDoDono(donoId, reservaId);
        if (reserva.getStatus() != StatusReserva.CONFIRMADA) {
            throw new RegraDeNegocioException("Só uma reserva confirmada pode ser marcada como falta.");
        }
        if (reserva.getInicio().isAfter(Instant.now())) {
            throw new RegraDeNegocioException("A falta só pode ser marcada depois do início do horário.");
        }
        reserva.marcarFalta();
        return item(donoId, reserva);
    }

    @Transactional
    public AgendaItem desfazerFalta(Long donoId, Long reservaId) {
        Reserva reserva = reservaDoDono(donoId, reservaId);
        if (reserva.getStatus() != StatusReserva.FALTOU) {
            throw new RegraDeNegocioException("Esta reserva não está marcada como falta.");
        }
        reserva.desfazerFalta();
        return item(donoId, reserva);
    }

    /** O dono pode cancelar a qualquer momento, inclusive liberar um bloqueio; o prazo vale só para o jogador. */
    @Transactional
    public AgendaItem cancelar(Long donoId, Long reservaId) {
        Reserva reserva = reservaDoDono(donoId, reservaId);
        if (reserva.getStatus() != StatusReserva.CONFIRMADA && reserva.getStatus() != StatusReserva.BLOQUEADA) {
            throw new RegraDeNegocioException("Esta reserva não pode ser cancelada.");
        }
        if (pagamentos.findFirstByReservaIdAndStatus(reservaId, StatusPagamento.PAGO).isPresent()) {
            throw new RegraDeNegocioException("Estorne o pagamento antes de cancelar.");
        }
        reserva.cancelar();
        return item(donoId, reserva);
    }

    @Transactional(readOnly = true)
    public ResumoFinanceiro resumo(Long donoId, LocalDate de, LocalDate ate) {
        validarPeriodo(de, ate, MAXIMO_DE_DIAS_NO_RESUMO);
        Empresa empresa = empresas.findByDonoId(donoId)
                .orElseThrow(() -> new NaoEncontradoException("Empresa ainda não cadastrada."));

        List<Reserva> doPeriodo = doPeriodo(empresa, de, ate).stream()
                .filter(r -> r.getStatus() != StatusReserva.BLOQUEADA)
                .toList();
        Map<Long, Pagamento> pagos = pagosPorReserva(doPeriodo);

        BigDecimal recebido = BigDecimal.ZERO;
        BigDecimal aReceber = BigDecimal.ZERO;
        BigDecimal perdido = BigDecimal.ZERO;
        Map<MetodoPagamento, BigDecimal> porMetodo = new EnumMap<>(MetodoPagamento.class);
        int faltas = 0;
        int canceladas = 0;
        int validas = 0;

        for (Reserva reserva : doPeriodo) {
            Pagamento pago = pagos.get(reserva.getId());
            if (pago != null) {
                recebido = recebido.add(pago.getValor());
                porMetodo.merge(pago.getMetodo(), pago.getValor(), BigDecimal::add);
            }
            switch (reserva.getStatus()) {
                case CANCELADA -> canceladas++;
                case FALTOU -> {
                    faltas++;
                    validas++;
                    if (pago == null) {
                        perdido = perdido.add(reserva.getPrecoTotal());
                    }
                }
                default -> {
                    validas++;
                    if (pago == null) {
                        aReceber = aReceber.add(reserva.getPrecoTotal());
                    }
                }
            }
        }
        return new ResumoFinanceiro(recebido, porMetodo, aReceber, perdido, validas, faltas, canceladas);
    }

    private AgendaItem salvar(Long donoId, Reserva nova) {
        try {
            return item(donoId, reservas.saveAndFlush(nova));
        } catch (DataIntegrityViolationException e) {
            throw new RegraDeNegocioException("Já existe uma reserva ou bloqueio neste horário.");
        }
    }

    /** Sempre com trava na linha e conferindo que a quadra é do dono: reserva de outra empresa se comporta como inexistente. */
    private Reserva reservaDoDono(Long donoId, Long reservaId) {
        Reserva reserva = reservas.travarPorId(reservaId)
                .orElseThrow(() -> new NaoEncontradoException("Reserva não encontrada."));
        try {
            quadraService.quadraDoDono(donoId, reserva.getQuadraId());
        } catch (NaoEncontradoException e) {
            throw new NaoEncontradoException("Reserva não encontrada.");
        }
        return reserva;
    }

    private void validarIntervalo(Instant inicio, Instant fim) {
        if (!fim.isAfter(inicio)) {
            throw new DadosInvalidosException("O fim deve ser depois do início.");
        }
        if (Duration.between(inicio, fim).compareTo(DURACAO_MAXIMA) > 0) {
            throw new DadosInvalidosException("O período pode ter no máximo 24 horas.");
        }
    }

    private void validarPeriodo(LocalDate de, LocalDate ate, int maximoDeDias) {
        if (ate.isBefore(de)) {
            throw new DadosInvalidosException("A data final deve ser igual ou posterior à inicial.");
        }
        if (ChronoUnit.DAYS.between(de, ate) >= maximoDeDias) {
            throw new DadosInvalidosException("O período pode ter no máximo " + maximoDeDias + " dias.");
        }
    }

    private List<Reserva> doPeriodo(Empresa empresa, LocalDate de, LocalDate ate) {
        ZoneId fuso = ZoneId.of(empresa.getFuso());
        List<Long> quadraIds = quadras.findByEmpresaIdOrderByNomeAsc(empresa.getId()).stream()
                .map(Quadra::getId)
                .toList();
        return reservas.findByQuadraIdInAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
                quadraIds, de.atStartOfDay(fuso).toInstant(), ate.plusDays(1).atStartOfDay(fuso).toInstant());
    }

    private Map<Long, Pagamento> pagosPorReserva(List<Reserva> lista) {
        return pagamentos.findByReservaIdInAndStatus(lista.stream().map(Reserva::getId).toList(), StatusPagamento.PAGO)
                .stream()
                .collect(Collectors.toMap(Pagamento::getReservaId, Function.identity(), (a, b) -> a));
    }

    private AgendaItem item(Long donoId, Reserva reserva) {
        Empresa empresa = empresas.findByDonoId(donoId).orElseThrow();
        return itens(empresa, List.of(reserva)).getFirst();
    }

    private List<AgendaItem> itens(Empresa empresa, List<Reserva> lista) {
        ZoneId fuso = ZoneId.of(empresa.getFuso());
        Map<Long, String> nomesDasQuadras = quadras.findByEmpresaIdOrderByNomeAsc(empresa.getId()).stream()
                .collect(Collectors.toMap(Quadra::getId, Quadra::getNome));
        Map<Long, Usuario> jogadores = usuarios
                .findAllById(lista.stream().map(Reserva::getUsuarioId).filter(id -> id != null).toList())
                .stream().collect(Collectors.toMap(Usuario::getId, Function.identity()));
        Map<Long, Pagamento> pagos = pagosPorReserva(lista);

        return lista.stream().map(reserva -> {
            ZonedDateTime inicio = reserva.getInicio().atZone(fuso);
            Usuario jogador = reserva.getUsuarioId() == null ? null : jogadores.get(reserva.getUsuarioId());
            Pagamento pago = pagos.get(reserva.getId());
            return new AgendaItem(
                    reserva.getId(),
                    reserva.getQuadraId(),
                    nomesDasQuadras.get(reserva.getQuadraId()),
                    reserva.getInicio(),
                    reserva.getFim(),
                    inicio.toLocalDate(),
                    inicio.toLocalTime(),
                    reserva.getFim().atZone(fuso).toLocalTime(),
                    jogador != null ? jogador.getNome() : reserva.getClienteNome(),
                    jogador != null ? jogador.getTelefone() : null,
                    jogador != null,
                    reserva.getPrecoTotal(),
                    reserva.getStatus(),
                    pago != null ? pago.getMetodo() : null);
        }).toList();
    }
}
