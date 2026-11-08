package br.com.playspot.busca;

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
class BuscaEReservaTests {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    JdbcTemplate jdbc;

    Usuario dono;
    Usuario jogador;
    long quadraId;
    long empresaId;
    short esporteId;
    LocalDate data = LocalDate.now(FUSO).plusDays(10);

    @BeforeEach
    void prepara() throws Exception {
        dono = usuarios.save(new Usuario("Dona", "dona@exemplo.com", "h", null, Papel.DONO));
        jogador = usuarios.save(new Usuario("Jo", "jo@exemplo.com", "h", null, Papel.JOGADOR));
        esporteId = jdbc.queryForObject("select id from esporte where nome = 'Padel'", Short.class);
        empresaId = id(salvaEmpresa(24).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        quadraId = id(mockMvc.perform(post("/api/dono/quadras").with(como(dono))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Q1","duracaoSlotMin":60,"ativa":true,"esporteIds":[%d]}
                                """.formatted(esporteId)))
                .andReturn().getResponse().getContentAsString());
        mockMvc.perform(post("/api/admin/empresas/" + empresaId + "/aprovacao")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
        abre(data);
    }

    private ResultActions salvaEmpresa(int prazo) throws Exception {
        return mockMvc.perform(put("/api/dono/empresa").with(como(dono))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Arena","endereco":"Rua","cidade":"São José","uf":"SC",
                         "prazoCancelamentoHoras":%d,"latitude":-27.6,"longitude":-48.63}
                        """.formatted(prazo)));
    }

    private void abre(LocalDate dia) throws Exception {
        mockMvc.perform(put("/api/dono/quadras/" + quadraId + "/horarios").with(como(dono))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"faixas":[{"diaSemana":%d,"horaInicio":"18:00","horaFim":"20:00","precoHora":100}]}
                                """.formatted(dia.getDayOfWeek().getValue() % 7)))
                .andExpect(status().isOk());
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

    /** 18:00 em São Paulo, em UTC. */
    private String as18h(LocalDate dia) {
        return dia + "T21:00:00Z";
    }

    private ResultActions reserva(Usuario usuario, String inicio) throws Exception {
        return mockMvc.perform(post("/api/reservas").with(como(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"quadraId":%d,"inicio":"%s"}
                        """.formatted(quadraId, inicio)));
    }

    private ResultActions busca(String... parametros) throws Exception {
        var pedido = get("/api/busca").param("data", data.toString());
        for (int i = 0; i < parametros.length; i += 2) {
            pedido = pedido.param(parametros[i], parametros[i + 1]);
        }
        return mockMvc.perform(pedido);
    }

    @Test
    void buscaPorCidadeIgnoraAcentoEMaiuscula() throws Exception {
        busca("cidade", "SAO JOSE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].horariosLivres").value(2))
                .andExpect(jsonPath("$[0].precoAPartirDe").value(100.00))
                .andExpect(jsonPath("$[0].distanciaKm").isEmpty());
    }

    @Test
    void buscaPertoDevolveDistancia() throws Exception {
        busca("latitude", "-27.59", "longitude", "-48.63")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].distanciaKm").value(org.hamcrest.Matchers.closeTo(1.11, 0.05)));
    }

    @Test
    void empresaForaDoRaioNaoAparece() throws Exception {
        busca("latitude", "-23.55", "longitude", "-46.63").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void filtraPorEsporte() throws Exception {
        short outro = jdbc.queryForObject("select id from esporte where nome = 'Basquete'", Short.class);
        busca("cidade", "São José", "esporteId", String.valueOf(esporteId)).andExpect(jsonPath("$.length()").value(1));
        busca("cidade", "São José", "esporteId", String.valueOf(outro)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void empresaSemHorarioLivreNaDataNaoAparece() throws Exception {
        reserva(jogador, as18h(data)).andExpect(status().isCreated());
        reserva(jogador, data + "T22:00:00Z").andExpect(status().isCreated());
        busca("cidade", "São José").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void buscaSemCidadeNemLocalizacaoDevolve400() throws Exception {
        busca().andExpect(status().isBadRequest());
    }

    @Test
    void paginaPublicaNaoExpoeDocumento() throws Exception {
        mockMvc.perform(get("/api/empresas/" + empresaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quadras.length()").value(1))
                .andExpect(jsonPath("$.documento").doesNotExist())
                .andExpect(jsonPath("$.donoId").doesNotExist());
    }

    @Test
    void reservaConfirmaNaHoraComPrecoCalculadoNoServidor() throws Exception {
        reserva(jogador, as18h(data))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"))
                .andExpect(jsonPath("$.precoTotal").value(100.00))
                .andExpect(jsonPath("$.fim").value(data + "T22:00:00Z"));

        mockMvc.perform(get("/api/quadras/" + quadraId + "/horarios").param("data", data.toString()))
                .andExpect(jsonPath("$[0].livre").value(false))
                .andExpect(jsonPath("$[1].livre").value(true));
    }

    @Test
    void mesmoHorarioNaoPodeSerReservadoDuasVezes() throws Exception {
        reserva(jogador, as18h(data)).andExpect(status().isCreated());
        reserva(jogador, as18h(data))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Este horário não está mais disponível."));
    }

    @Test
    void inicioForaDaGradeDeHorariosERecusado() throws Exception {
        reserva(jogador, data + "T21:30:00Z").andExpect(status().isConflict());
    }

    @Test
    void reservaAlemDe60DiasERecusada() throws Exception {
        LocalDate distante = LocalDate.now(FUSO).plusDays(61);
        abre(distante);
        reserva(jogador, as18h(distante)).andExpect(status().isConflict());
    }

    @Test
    void reservaNoLimiteDe60DiasEAceita() throws Exception {
        LocalDate limite = LocalDate.now(FUSO).plusDays(60);
        abre(limite);
        reserva(jogador, as18h(limite)).andExpect(status().isCreated());
    }

    @Test
    void donoNaoReserva() throws Exception {
        reserva(dono, as18h(data)).andExpect(status().isForbidden());
    }

    @Test
    void reservarExigeLogin() throws Exception {
        mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelarDentroDoPrazoLiberaOHorario() throws Exception {
        long reservaId = id(reserva(jogador, as18h(data)).andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/api/reservas/" + reservaId + "/cancelamento").with(como(jogador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.podeCancelar").value(false));

        mockMvc.perform(get("/api/quadras/" + quadraId + "/horarios").param("data", data.toString()))
                .andExpect(jsonPath("$[0].livre").value(true));
    }

    @Test
    void cancelarForaDoPrazoERecusado() throws Exception {
        salvaEmpresa(720).andExpect(status().isOk());
        long reservaId = id(reserva(jogador, as18h(data)).andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/api/reservas/" + reservaId + "/cancelamento").with(como(jogador)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("O prazo para cancelar passou. Fale com o local."));
    }

    @Test
    void naoCancelaReservaDeOutroJogador() throws Exception {
        Usuario outro = usuarios.save(new Usuario("Ou", "ou@exemplo.com", "h", null, Papel.JOGADOR));
        long reservaId = id(reserva(jogador, as18h(data)).andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/api/reservas/" + reservaId + "/cancelamento").with(como(outro)))
                .andExpect(status().isNotFound());
    }

    @Test
    void minhasReservasListaSoAsDoJogador() throws Exception {
        Usuario outro = usuarios.save(new Usuario("Ou", "ou@exemplo.com", "h", null, Papel.JOGADOR));
        reserva(jogador, as18h(data)).andExpect(status().isCreated());

        mockMvc.perform(get("/api/reservas/minhas").with(como(jogador)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].empresaNome").value("Arena"))
                .andExpect(jsonPath("$[0].podeCancelar").value(true));
        mockMvc.perform(get("/api/reservas/minhas").with(como(outro))).andExpect(jsonPath("$.length()").value(0));
    }
}
