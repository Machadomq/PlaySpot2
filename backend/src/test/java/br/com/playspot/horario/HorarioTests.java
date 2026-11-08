package br.com.playspot.horario;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import br.com.playspot.usuario.Papel;
import br.com.playspot.usuario.Usuario;
import br.com.playspot.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HorarioTests {

    private static final String EMPRESA = """
            {"nome":"Arena","endereco":"Rua","cidade":"Blumenau","uf":"SC","prazoCancelamentoHoras":24}
            """;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    JdbcTemplate jdbc;

    Usuario dono;
    long quadraId;
    /** Uma data futura e o número do dia da semana dela (0 = domingo). */
    LocalDate data = LocalDate.now().plusMonths(1);
    int dia = data.getDayOfWeek().getValue() % 7;

    @BeforeEach
    void prepara() throws Exception {
        dono = usuarios.save(new Usuario("Dona", "dona@exemplo.com", "h", null, Papel.DONO));
        mockMvc.perform(put("/api/dono/empresa").with(como(dono))
                        .contentType(MediaType.APPLICATION_JSON).content(EMPRESA))
                .andExpect(status().isOk());
        short esporteId = jdbc.queryForObject("select id from esporte where nome = 'Padel'", Short.class);
        String corpo = mockMvc.perform(post("/api/dono/quadras").with(como(dono))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Q1","duracaoSlotMin":60,"ativa":true,"esporteIds":[%d]}
                                """.formatted(esporteId)))
                .andReturn().getResponse().getContentAsString();
        Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(corpo);
        m.find();
        quadraId = Long.parseLong(m.group(1));
    }

    private RequestPostProcessor como(Usuario usuario) {
        return jwt().jwt(j -> j.subject(String.valueOf(usuario.getId())))
                .authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getPapel()));
    }

    private String faixa(int diaSemana, String inicio, String fim, String preco) {
        return """
                {"diaSemana":%d,"horaInicio":"%s","horaFim":"%s","precoHora":%s}
                """.formatted(diaSemana, inicio, fim, preco);
    }

    private org.springframework.test.web.servlet.ResultActions salva(Usuario usuario, String... faixas) throws Exception {
        return mockMvc.perform(put("/api/dono/quadras/" + quadraId + "/horarios").with(como(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"faixas\":[" + String.join(",", faixas) + "]}"));
    }

    private void reserva(String inicioLocal, String fimLocal, String status) {
        jdbc.update("insert into reserva(quadra_id, cliente_nome, inicio, fim, preco_total, status) "
                + "values (?, 'Cliente', (?::timestamp at time zone 'America/Sao_Paulo'), "
                + "(?::timestamp at time zone 'America/Sao_Paulo'), 100, ?)",
                quadraId, data + " " + inicioLocal, data + " " + fimLocal, status);
    }

    private org.springframework.test.web.servlet.ResultActions consulta(LocalDate dataConsulta) throws Exception {
        return mockMvc.perform(get("/api/quadras/" + quadraId + "/horarios").param("data", dataConsulta.toString()));
    }

    @Test
    void salvarSubstituiASemanaInteira() throws Exception {
        salva(dono, faixa(1, "08:00", "12:00", "80")).andExpect(status().isOk());
        salva(dono, faixa(2, "18:00", "00:00", "120")).andExpect(status().isOk());

        mockMvc.perform(get("/api/dono/quadras/" + quadraId + "/horarios").with(como(dono)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].diaSemana").value(2));
    }

    @Test
    void recusaFaixasSobrepostas() throws Exception {
        salva(dono, faixa(1, "08:00", "12:00", "80"), faixa(1, "11:00", "13:00", "90"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Há faixas sobrepostas na segunda."));
    }

    @Test
    void aceitaFaixasEncostadas() throws Exception {
        salva(dono, faixa(1, "08:00", "12:00", "80"), faixa(1, "12:00", "13:00", "90")).andExpect(status().isOk());
    }

    @Test
    void recusaFimAntesDoInicio() throws Exception {
        salva(dono, faixa(1, "12:00", "08:00", "80")).andExpect(status().isBadRequest());
    }

    @Test
    void recusaPrecoNegativo() throws Exception {
        salva(dono, faixa(1, "08:00", "12:00", "-1")).andExpect(status().isBadRequest());
    }

    @Test
    void outroDonoNaoAlteraHorarios() throws Exception {
        Usuario outro = usuarios.save(new Usuario("Outro", "outro@exemplo.com", "h", null, Papel.DONO));
        salva(outro, faixa(1, "08:00", "12:00", "80")).andExpect(status().isNotFound());
    }

    @Test
    void horariosUsamOFusoDaEmpresa() throws Exception {
        salva(dono, faixa(dia, "18:00", "19:00", "100")).andExpect(status().isOk());

        consulta(data)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].inicio").value(data + "T21:00:00Z"))
                .andExpect(jsonPath("$[0].preco").value(100.00))
                .andExpect(jsonPath("$[0].livre").value(true));
    }

    @Test
    void horarioReservadoFicaOcupado() throws Exception {
        salva(dono, faixa(dia, "18:00", "20:00", "100")).andExpect(status().isOk());
        reserva("18:00", "19:00", "CONFIRMADA");

        consulta(data)
                .andExpect(jsonPath("$[0].livre").value(false))
                .andExpect(jsonPath("$[1].livre").value(true));
    }

    @Test
    void reservaCanceladaLiberaOHorario() throws Exception {
        salva(dono, faixa(dia, "18:00", "19:00", "100")).andExpect(status().isOk());
        reserva("18:00", "19:00", "CANCELADA");

        consulta(data).andExpect(jsonPath("$[0].livre").value(true));
    }

    @Test
    void dataPassadaNaoTemHorarioLivre() throws Exception {
        LocalDate passada = data.minusWeeks(52);
        int diaPassado = passada.getDayOfWeek().getValue() % 7;
        salva(dono, faixa(diaPassado, "18:00", "19:00", "100")).andExpect(status().isOk());

        consulta(passada).andExpect(jsonPath("$[0].livre").value(false));
    }

    @Test
    void diaSemFaixasNaoTemHorarios() throws Exception {
        consulta(data).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void quadraInativaNaoAparece() throws Exception {
        short esporteId = jdbc.queryForObject("select id from esporte where nome = 'Padel'", Short.class);
        mockMvc.perform(put("/api/dono/quadras/" + quadraId).with(como(dono))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Q1","duracaoSlotMin":60,"ativa":false,"esporteIds":[%d]}
                                """.formatted(esporteId)))
                .andExpect(status().isOk());

        consulta(data).andExpect(status().isNotFound());
    }
}
