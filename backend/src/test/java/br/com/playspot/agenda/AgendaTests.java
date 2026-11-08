package br.com.playspot.agenda;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import br.com.playspot.usuario.Papel;
import br.com.playspot.usuario.Usuario;
import br.com.playspot.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgendaTests {

    private static final String EMPRESA = """
            {"nome":"Arena","endereco":"Rua","cidade":"Blumenau","uf":"SC","prazoCancelamentoHoras":24,
             "latitude":-26.9,"longitude":-49.06}
            """;
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    JdbcTemplate jdbc;

    Usuario dono;
    long quadraId;
    LocalDate data = LocalDate.now(FUSO).plusDays(5);

    @BeforeEach
    void prepara() throws Exception {
        dono = usuarios.save(new Usuario("Dona", "dona@exemplo.com", "h", null, Papel.DONO));
        quadraId = criaQuadra(dono);
    }

    private long criaQuadra(Usuario usuario) throws Exception {
        mockMvc.perform(put("/api/dono/empresa").with(como(usuario))
                        .contentType(MediaType.APPLICATION_JSON).content(EMPRESA))
                .andExpect(status().isOk());
        short esporteId = jdbc.queryForObject("select id from esporte where nome = 'Padel'", Short.class);
        return id(mockMvc.perform(post("/api/dono/quadras").with(como(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Q1","duracaoSlotMin":60,"ativa":true,"esporteIds":[%d]}
                                """.formatted(esporteId)))
                .andReturn().getResponse().getContentAsString());
    }

    private RequestPostProcessor como(Usuario usuario) {
        return jwt().jwt(j -> j.subject(String.valueOf(usuario.getId())))
                .authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getPapel()));
    }

    private long id(String json) {
        Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(json);
        m.find();
        return Long.parseLong(m.group(1));
    }

    private ResultActions manual(LocalDate dia, int horaUtc, String preco) throws Exception {
        return mockMvc.perform(post("/api/dono/reservas").with(como(dono))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"quadraId":%d,"inicio":"%sT%02d:00:00Z","fim":"%sT%02d:00:00Z","clienteNome":"Cliente Zap","preco":%s}
                        """.formatted(quadraId, dia, horaUtc, dia, horaUtc + 1, preco)));
    }

    private long reservaManual(int horaUtc, String preco) throws Exception {
        return id(manual(data, horaUtc, preco).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private ResultActions acao(long reservaId, String caminho) throws Exception {
        return mockMvc.perform(post("/api/dono/reservas/" + reservaId + caminho).with(como(dono)));
    }

    private ResultActions pagar(long reservaId, String metodo) throws Exception {
        return mockMvc.perform(post("/api/dono/reservas/" + reservaId + "/pagamento").with(como(dono))
                .contentType(MediaType.APPLICATION_JSON).content("{\"metodo\":\"" + metodo + "\"}"));
    }

    private ResultActions resumo() throws Exception {
        return mockMvc.perform(get("/api/dono/financeiro").with(como(dono))
                .param("de", data.minusDays(10).toString()).param("ate", data.toString()));
    }

    private ResultActions agenda() throws Exception {
        return mockMvc.perform(get("/api/dono/agenda").with(como(dono))
                .param("de", data.toString()).param("ate", data.toString()));
    }

    private long reservaNoPassado() {
        return jdbc.queryForObject("insert into reserva(quadra_id, cliente_nome, inicio, fim, preco_total, status) "
                + "values (?, 'Passado', now() - interval '2 hours', now() - interval '1 hour', 80, 'CONFIRMADA') returning id",
                Long.class, quadraId);
    }

    @Test
    void reservaManualApareceNaAgendaComHoraDaEmpresa() throws Exception {
        reservaManual(21, "90");
        agenda()
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cliente").value("Cliente Zap"))
                .andExpect(jsonPath("$[0].peloApp").value(false))
                .andExpect(jsonPath("$[0].horaInicio").value("18:00:00"))
                .andExpect(jsonPath("$[0].data").value(data.toString()));
    }

    @Test
    void reservaManualEmHorarioOcupadoERecusada() throws Exception {
        reservaManual(21, "90");
        manual(data, 21, "90")
                .andExpect(status().isConflict());
    }

    @Test
    void bloqueioOcupaHorarioENaoEntraNoFinanceiro() throws Exception {
        mockMvc.perform(post("/api/dono/bloqueios").with(como(dono))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quadraId":%d,"inicio":"%sT21:00:00Z","fim":"%sT22:00:00Z","motivo":"Manutenção"}
                                """.formatted(quadraId, data, data)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BLOQUEADA"));

        resumo().andExpect(jsonPath("$.reservas").value(0));
        manual(data, 21, "90").andExpect(status().isConflict());
    }

    @Test
    void liberarBloqueioDevolveOHorario() throws Exception {
        long bloqueio = id(mockMvc.perform(post("/api/dono/bloqueios").with(como(dono))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quadraId":%d,"inicio":"%sT21:00:00Z","fim":"%sT22:00:00Z","motivo":"Chuva"}
                                """.formatted(quadraId, data, data)))
                .andReturn().getResponse().getContentAsString());

        acao(bloqueio, "/cancelamento").andExpect(status().isOk());
        agenda().andExpect(jsonPath("$.length()").value(0));
        manual(data, 21, "90").andExpect(status().isCreated());
    }

    @Test
    void fimAntesDoInicioDevolve400() throws Exception {
        mockMvc.perform(post("/api/dono/reservas").with(como(dono))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quadraId":%d,"inicio":"%sT22:00:00Z","fim":"%sT21:00:00Z","clienteNome":"X","preco":10}
                                """.formatted(quadraId, data, data)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pagamentoDuplicadoERecusado() throws Exception {
        long reserva = reservaManual(21, "90");
        pagar(reserva, "PIX").andExpect(status().isOk()).andExpect(jsonPath("$.metodoPago").value("PIX"));
        pagar(reserva, "PIX").andExpect(status().isConflict());
    }

    @Test
    void estornoPermiteNovoPagamento() throws Exception {
        long reserva = reservaManual(21, "90");
        pagar(reserva, "PIX").andExpect(status().isOk());
        acao(reserva, "/estorno").andExpect(status().isOk()).andExpect(jsonPath("$.metodoPago").isEmpty());
        pagar(reserva, "DINHEIRO").andExpect(status().isOk());
    }

    @Test
    void reservaPagaPrecisaDeEstornoParaCancelar() throws Exception {
        long reserva = reservaManual(21, "90");
        pagar(reserva, "CARTAO").andExpect(status().isOk());

        acao(reserva, "/cancelamento")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Estorne o pagamento antes de cancelar."));

        acao(reserva, "/estorno").andExpect(status().isOk());
        acao(reserva, "/cancelamento").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADA"));
    }

    @Test
    void faltaAntesDoHorarioERecusada() throws Exception {
        acao(reservaManual(21, "90"), "/falta").andExpect(status().isConflict());
    }

    @Test
    void faltaDepoisDoInicioPodeSerDesfeita() throws Exception {
        long reserva = reservaNoPassado();
        acao(reserva, "/falta").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FALTOU"));
        acao(reserva, "/falta/desfazer").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMADA"));
    }

    @Test
    void resumoSomaCadaSituacao() throws Exception {
        pagar(reservaManual(18, "100"), "PIX").andExpect(status().isOk());
        pagar(reservaManual(19, "50"), "DINHEIRO").andExpect(status().isOk());
        reservaManual(20, "70");
        acao(reservaManual(21, "30"), "/cancelamento").andExpect(status().isOk());

        resumo()
                .andExpect(jsonPath("$.recebido", closeTo(150.0, 0.001), Double.class))
                .andExpect(jsonPath("$.recebidoPorMetodo.PIX", closeTo(100.0, 0.001), Double.class))
                .andExpect(jsonPath("$.recebidoPorMetodo.DINHEIRO", closeTo(50.0, 0.001), Double.class))
                .andExpect(jsonPath("$.aReceber", closeTo(70.0, 0.001), Double.class))
                .andExpect(jsonPath("$.reservas").value(3))
                .andExpect(jsonPath("$.canceladas").value(1));
    }

    @Test
    void faltaSemPagamentoContaComoPerdido() throws Exception {
        long reserva = reservaNoPassado();
        acao(reserva, "/falta").andExpect(status().isOk());

        mockMvc.perform(get("/api/dono/financeiro").with(como(dono))
                        .param("de", LocalDate.now(FUSO).minusDays(1).toString())
                        .param("ate", LocalDate.now(FUSO).plusDays(1).toString()))
                .andExpect(jsonPath("$.perdidoEmFaltas", closeTo(80.0, 0.001), Double.class))
                .andExpect(jsonPath("$.faltas").value(1))
                .andExpect(jsonPath("$.aReceber", closeTo(0.0, 0.001), Double.class));
    }

    @Test
    void estornoSaiDoRecebido() throws Exception {
        long reserva = reservaManual(21, "90");
        pagar(reserva, "PIX").andExpect(status().isOk());
        acao(reserva, "/estorno").andExpect(status().isOk());

        resumo()
                .andExpect(jsonPath("$.recebido", closeTo(0.0, 0.001), Double.class))
                .andExpect(jsonPath("$.aReceber", closeTo(90.0, 0.001), Double.class));
    }

    @Test
    void outroDonoNaoAcessaAReserva() throws Exception {
        Usuario outro = usuarios.save(new Usuario("Outro", "outro@exemplo.com", "h", null, Papel.DONO));
        criaQuadra(outro);
        long reserva = reservaManual(21, "90");

        mockMvc.perform(post("/api/dono/reservas/" + reserva + "/cancelamento").with(como(outro)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/dono/agenda").with(como(outro))
                        .param("de", data.toString()).param("ate", data.toString()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void outroDonoNaoCriaReservaNaQuadraAlheia() throws Exception {
        Usuario outro = usuarios.save(new Usuario("Outro", "outro@exemplo.com", "h", null, Papel.DONO));
        criaQuadra(outro);

        mockMvc.perform(post("/api/dono/reservas").with(como(outro))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quadraId":%d,"inicio":"%sT21:00:00Z","fim":"%sT22:00:00Z","clienteNome":"X","preco":10}
                                """.formatted(quadraId, data, data)))
                .andExpect(status().isNotFound());
    }

    @Test
    void periodoDaAgendaTemLimite() throws Exception {
        mockMvc.perform(get("/api/dono/agenda").with(como(dono))
                        .param("de", data.toString()).param("ate", data.plusDays(31).toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jogadorNaoCancelaReservaJaPaga() throws Exception {
        Usuario jogador = usuarios.save(new Usuario("Jo", "jo@exemplo.com", "h", null, Papel.JOGADOR));
        long reserva = jdbc.queryForObject(
                "insert into reserva(usuario_id, quadra_id, inicio, fim, preco_total, status) "
                        + "values (?, ?, now() + interval '5 days', now() + interval '5 days 1 hour', 80, 'CONFIRMADA') returning id",
                Long.class, jogador.getId(), quadraId);
        pagar(reserva, "PIX").andExpect(status().isOk());

        mockMvc.perform(get("/api/reservas/minhas").with(como(jogador)))
                .andExpect(jsonPath("$[0].podeCancelar").value(false));
        mockMvc.perform(post("/api/reservas/" + reserva + "/cancelamento").with(como(jogador)))
                .andExpect(status().isConflict());
    }

    @Test
    void agendaMostraNomeDoJogadorDoApp() throws Exception {
        Usuario jogador = usuarios.save(new Usuario("Joana", "joana@exemplo.com", "h", "47999990000", Papel.JOGADOR));
        jdbc.update("insert into reserva(usuario_id, quadra_id, inicio, fim, preco_total, status) "
                + "values (?, ?, ?::timestamptz, ?::timestamptz, 80, 'CONFIRMADA')",
                jogador.getId(), quadraId, data + "T21:00:00Z", data + "T22:00:00Z");

        agenda()
                .andExpect(jsonPath("$[0].cliente").value("Joana"))
                .andExpect(jsonPath("$[0].telefone").value("47999990000"))
                .andExpect(jsonPath("$[0].peloApp").value(true));
    }
}
