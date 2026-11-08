package br.com.playspot.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.security.crypto.password.PasswordEncoder;
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
class AdminTests {

    private static final String SENHA = "senha-segura-123";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JdbcTemplate jdbc;

    Usuario dono;
    Usuario jogador;
    long empresaId;
    long quadraId;
    short padel;
    LocalDate data = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(5);

    @BeforeEach
    void prepara() throws Exception {
        dono = usuarios.save(new Usuario("Dona", "dona@exemplo.com", passwordEncoder.encode(SENHA), null, Papel.DONO));
        jogador = usuarios.save(new Usuario("Jo", "jo@exemplo.com", passwordEncoder.encode(SENHA), null, Papel.JOGADOR));
        padel = jdbc.queryForObject("select id from esporte where nome = 'Padel'", Short.class);
        empresaId = id(mockMvc.perform(put("/api/dono/empresa").with(como(dono))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Arena","endereco":"Rua","cidade":"Gaspar","uf":"SC","prazoCancelamentoHoras":24,
                                 "latitude":-26.9,"longitude":-48.9}
                                """))
                .andReturn().getResponse().getContentAsString());
        quadraId = id(mockMvc.perform(post("/api/dono/quadras").with(como(dono))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Q1","duracaoSlotMin":60,"ativa":true,"esporteIds":[%d]}
                                """.formatted(padel)))
                .andReturn().getResponse().getContentAsString());
        mockMvc.perform(put("/api/dono/quadras/" + quadraId + "/horarios").with(como(dono))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"faixas":[{"diaSemana":%d,"horaInicio":"18:00","horaFim":"19:00","precoHora":100}]}
                                """.formatted(data.getDayOfWeek().getValue() % 7)))
                .andExpect(status().isOk());
    }

    private RequestPostProcessor como(Usuario usuario) {
        return jwt().jwt(j -> j.subject(String.valueOf(usuario.getId())))
                .authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getPapel()));
    }

    private RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private long id(String json) {
        Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(json);
        m.find();
        return Long.parseLong(m.group(1));
    }

    private ResultActions buscaNaCidade() throws Exception {
        return mockMvc.perform(get("/api/busca").param("cidade", "Gaspar").param("data", data.toString()));
    }

    private ResultActions acaoEmpresa(String acao) throws Exception {
        return mockMvc.perform(post("/api/admin/empresas/" + empresaId + "/" + acao).with(admin()));
    }

    private ResultActions criaEsporte(String nome) throws Exception {
        return mockMvc.perform(post("/api/admin/esportes").with(admin())
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"" + nome + "\"}"));
    }

    private ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"));
    }

    private ResultActions trocaSenha(String atual, String nova) throws Exception {
        return mockMvc.perform(post("/api/me/senha").with(como(jogador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"senhaAtual\":\"" + atual + "\",\"novaSenha\":\"" + nova + "\"}"));
    }

    @Test
    void soAdminAcessaOPainel() throws Exception {
        mockMvc.perform(get("/api/admin/empresas").with(como(dono))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/empresas").with(como(jogador))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/empresas")).andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroNaoCriaAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"email\":\"x@exemplo.com\",\"senha\":\"" + SENHA + "\",\"papel\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void empresaNovaFicaInvisivelAteAprovar() throws Exception {
        buscaNaCidade().andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/empresas/" + empresaId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/quadras/" + quadraId + "/horarios").param("data", data.toString()))
                .andExpect(status().isNotFound());

        acaoEmpresa("aprovacao").andExpect(status().isOk()).andExpect(jsonPath("$.aprovada").value(true));

        buscaNaCidade().andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/empresas/" + empresaId)).andExpect(status().isOk());
    }

    @Test
    void jogadorNaoReservaEmEmpresaNaoAprovada() throws Exception {
        mockMvc.perform(post("/api/reservas").with(como(jogador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quadraId":%d,"inicio":"%sT21:00:00Z"}
                                """.formatted(quadraId, data)))
                .andExpect(status().isNotFound());
    }

    @Test
    void donoUsaAPropriaGradeAntesDaAprovacao() throws Exception {
        mockMvc.perform(get("/api/dono/quadras/" + quadraId + "/grade").with(como(dono)).param("data", data.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void outroDonoNaoVeAGrade() throws Exception {
        Usuario outro = usuarios.save(new Usuario("Ou", "ou@exemplo.com", "h", null, Papel.DONO));
        mockMvc.perform(get("/api/dono/quadras/" + quadraId + "/grade").with(como(outro)).param("data", data.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void revogarTiraDaBusca() throws Exception {
        acaoEmpresa("aprovacao").andExpect(status().isOk());
        acaoEmpresa("revogacao").andExpect(status().isOk());
        buscaNaCidade().andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void painelMostraPendentesPrimeiroComDadosDoDono() throws Exception {
        mockMvc.perform(get("/api/admin/empresas").with(admin()))
                .andExpect(jsonPath("$[0].aprovada").value(false))
                .andExpect(jsonPath("$[?(@.id == " + empresaId + ")].donoEmail").value("dona@exemplo.com"));
    }

    @Test
    void esporteNovoApareceNaLista() throws Exception {
        criaEsporte("Pickleball").andExpect(status().isCreated());
        mockMvc.perform(get("/api/esportes"))
                .andExpect(jsonPath("$[?(@.nome == 'Pickleball')]").isNotEmpty());
    }

    @Test
    void esporteRepetidoERecusadoIgnorandoMaiusculas() throws Exception {
        criaEsporte("PADEL").andExpect(status().isConflict());
    }

    @Test
    void renomeiaEsporte() throws Exception {
        mockMvc.perform(put("/api/admin/esportes/" + padel).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Padel Pro\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Padel Pro"));
    }

    @Test
    void naoRemoveEsporteEmUso() throws Exception {
        mockMvc.perform(delete("/api/admin/esportes/" + padel).with(admin())).andExpect(status().isConflict());
    }

    @Test
    void removeEsporteSemUso() throws Exception {
        long novo = id(criaEsporte("Pickleball").andReturn().getResponse().getContentAsString());
        mockMvc.perform(delete("/api/admin/esportes/" + novo).with(admin())).andExpect(status().isNoContent());
    }

    @Test
    void metricasContamPorSituacao() throws Exception {
        jdbc.update("insert into reserva(quadra_id, cliente_nome, inicio, fim, preco_total, status) values "
                + "(?, 'A', now() - interval '2 days', now() - interval '47 hours', 10, 'FALTOU')", quadraId);

        mockMvc.perform(get("/api/admin/metricas").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faltas").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.empresasPendentes").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void senhaTemporariaSubstituiASenhaAntiga() throws Exception {
        String resposta = mockMvc.perform(post("/api/admin/usuarios/senha-temporaria").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"JO@exemplo.com\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher m = Pattern.compile("\"senhaTemporaria\":\"([^\"]+)\"").matcher(resposta);
        m.find();

        login("jo@exemplo.com", m.group(1)).andExpect(status().isOk());
        login("jo@exemplo.com", SENHA).andExpect(status().isUnauthorized());
    }

    @Test
    void senhaTemporariaNaoValeParaAdmin() throws Exception {
        usuarios.save(new Usuario("Adm", "adm@exemplo.com", "h", null, Papel.ADMIN));
        mockMvc.perform(post("/api/admin/usuarios/senha-temporaria").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"adm@exemplo.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void trocarSenhaExigeASenhaAtual() throws Exception {
        trocaSenha("senha-errada-1", "nova-senha-123").andExpect(status().isBadRequest());
        login("jo@exemplo.com", SENHA).andExpect(status().isOk());
    }

    @Test
    void trocarSenhaPassaAValerNoLogin() throws Exception {
        trocaSenha(SENHA, "nova-senha-123").andExpect(status().isNoContent());
        login("jo@exemplo.com", "nova-senha-123").andExpect(status().isOk());
    }
}
