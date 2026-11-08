package br.com.playspot.empresa;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
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
class EmpresaEQuadraTests {

    private static final String EMPRESA = """
            {"nome":"Arena Azul","endereco":"Rua A, 10","cidade":"Blumenau","uf":"SC","prazoCancelamentoHoras":24}
            """;
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0};

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    JdbcTemplate jdbc;

    Usuario dono;
    Usuario outroDono;
    short esporteId;

    @BeforeEach
    void prepara() {
        dono = usuarios.save(new Usuario("Dona", "dona@exemplo.com", "h", null, Papel.DONO));
        outroDono = usuarios.save(new Usuario("Outro", "outro@exemplo.com", "h", null, Papel.DONO));
        esporteId = jdbc.queryForObject("select id from esporte where nome = 'Padel'", Short.class);
    }

    private RequestPostProcessor como(Usuario usuario) {
        return jwt().jwt(j -> j.subject(String.valueOf(usuario.getId())))
                .authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getPapel()));
    }

    private void criaEmpresa(Usuario usuario) throws Exception {
        mockMvc.perform(put("/api/dono/empresa").with(como(usuario))
                        .contentType(MediaType.APPLICATION_JSON).content(EMPRESA))
                .andExpect(status().isOk());
    }

    private String quadra(String nome, Object esporte) {
        return """
                {"nome":"%s","duracaoSlotMin":60,"ativa":true,"esporteIds":[%s]}
                """.formatted(nome, esporte);
    }

    private long criaQuadra(Usuario usuario) throws Exception {
        String corpo = mockMvc.perform(post("/api/dono/quadras").with(como(usuario))
                        .contentType(MediaType.APPLICATION_JSON).content(quadra("Quadra 1", esporteId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return id(corpo, "id");
    }

    private long id(String json, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\":\\[?(\\d+)").matcher(json);
        m.find();
        return Long.parseLong(m.group(1));
    }

    private org.springframework.test.web.servlet.ResultActions enviaFoto(long quadraId, byte[] bytes) throws Exception {
        return mockMvc.perform(multipart("/api/dono/quadras/" + quadraId + "/fotos")
                .file(new MockMultipartFile("arquivo", "foto.png", "image/png", bytes))
                .with(como(dono)));
    }

    @Test
    void jogadorNaoAcessaRotasDeDono() throws Exception {
        Usuario jogador = usuarios.save(new Usuario("Jo", "jo@exemplo.com", "h", null, Papel.JOGADOR));
        mockMvc.perform(get("/api/dono/empresa").with(como(jogador))).andExpect(status().isForbidden());
    }

    @Test
    void empresaInexistenteDevolve404() throws Exception {
        mockMvc.perform(get("/api/dono/empresa").with(como(dono))).andExpect(status().isNotFound());
    }

    @Test
    void salvarDuasVezesMantemUmaUnicaEmpresa() throws Exception {
        criaEmpresa(dono);
        mockMvc.perform(put("/api/dono/empresa").with(como(dono))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMPRESA.replace("Arena Azul", "Arena Verde")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dono/empresa").with(como(dono)))
                .andExpect(jsonPath("$.nome").value("Arena Verde"));
        org.assertj.core.api.Assertions.assertThat(
                jdbc.queryForObject("select count(*) from empresa where dono_id = ?", Integer.class, dono.getId()))
                .isEqualTo(1);
    }

    @Test
    void empresaComUfInvalidaDevolve400() throws Exception {
        mockMvc.perform(put("/api/dono/empresa").with(como(dono))
                        .contentType(MediaType.APPLICATION_JSON).content(EMPRESA.replace("\"SC\"", "\"sc\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.uf").exists());
    }

    @Test
    void naoCriaQuadraSemEmpresa() throws Exception {
        mockMvc.perform(post("/api/dono/quadras").with(como(dono))
                        .contentType(MediaType.APPLICATION_JSON).content(quadra("Q", esporteId)))
                .andExpect(status().isConflict());
    }

    @Test
    void criaEListaQuadraComEsportes() throws Exception {
        criaEmpresa(dono);
        criaQuadra(dono);
        mockMvc.perform(get("/api/dono/quadras").with(como(dono)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].esportes[0].nome").value("Padel"));
    }

    @Test
    void esporteInexistenteDevolve400() throws Exception {
        criaEmpresa(dono);
        mockMvc.perform(post("/api/dono/quadras").with(como(dono))
                        .contentType(MediaType.APPLICATION_JSON).content(quadra("Q", 9999)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void outroDonoNaoAlteraQuadraAlheia() throws Exception {
        criaEmpresa(dono);
        criaEmpresa(outroDono);
        long quadraId = criaQuadra(dono);

        mockMvc.perform(put("/api/dono/quadras/" + quadraId).with(como(outroDono))
                        .contentType(MediaType.APPLICATION_JSON).content(quadra("Roubada", esporteId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void fotoValidaFicaPublicaEPodeSerRemovida() throws Exception {
        criaEmpresa(dono);
        long quadraId = criaQuadra(dono);

        String corpo = enviaFoto(quadraId, PNG)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoIds.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        long fotoId = id(corpo, "fotoIds");

        mockMvc.perform(get("/api/fotos/" + fotoId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG));

        mockMvc.perform(delete("/api/dono/quadras/" + quadraId + "/fotos/" + fotoId).with(como(dono)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoIds.length()").value(0));
    }

    @Test
    void arquivoQueNaoEImagemDevolve400() throws Exception {
        criaEmpresa(dono);
        enviaFoto(criaQuadra(dono), "<script>alert(1)</script>".getBytes()).andExpect(status().isBadRequest());
    }

    @Test
    void sextaFotoERecusada() throws Exception {
        criaEmpresa(dono);
        long quadraId = criaQuadra(dono);
        for (int i = 0; i < 5; i++) {
            enviaFoto(quadraId, PNG).andExpect(status().isOk());
        }
        enviaFoto(quadraId, PNG).andExpect(status().isConflict());
    }

    @Test
    void bancoAceitaFaixaAteMeiaNoiteERecusaSobreposicao() throws Exception {
        criaEmpresa(dono);
        long quadraId = criaQuadra(dono);
        String insere = "insert into funcionamento(quadra_id, dia_semana, hora_inicio, hora_fim, preco_hora) "
                + "values (?, 1, ?::time, ?::time, 100)";

        jdbc.update(insere, quadraId, "18:00", "00:00");

        assertThatThrownBy(() -> jdbc.update(insere, quadraId, "23:00", "23:30"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void bancoAceitaReservaDeClienteSemConta() throws Exception {
        criaEmpresa(dono);
        long quadraId = criaQuadra(dono);
        jdbc.update("insert into reserva(quadra_id, cliente_nome, inicio, fim, preco_total, status) "
                + "values (?, 'Cliente do WhatsApp', now(), now() + interval '1 hour', 100, 'FALTOU')", quadraId);
    }
}
