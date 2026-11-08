package br.com.playspot.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginControllerTests {

    private static final String EMAIL = "login@exemplo.com";
    private static final String SENHA = "senha-segura-123";

    @Autowired
    MockMvc mockMvc;

    @BeforeEach
    void cadastraUsuario() throws Exception {
        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Ana","email":"%s","senha":"%s","papel":"DONO"}
                                """.formatted(EMAIL, SENHA)))
                .andExpect(status().isCreated());
    }

    private String login(String email, String senha) {
        return """
                {"email":"%s","senha":"%s"}
                """.formatted(email, senha);
    }

    @Test
    void loginComCredenciaisCorretasDevolveToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("LOGIN@exemplo.com", SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario.email").value(EMAIL))
                .andExpect(jsonPath("$.usuario.papel").value("DONO"));
    }

    @Test
    void senhaErradaDevolve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login(EMAIL, "senha-errada-123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emailInexistenteDevolve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("naoexiste@exemplo.com", SENHA)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotaProtegidaSemTokenDevolve401() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void rotaProtegidaComTokenDevolveOUsuario() throws Exception {
        String resposta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login(EMAIL, SENHA)))
                .andReturn().getResponse().getContentAsString();

        Matcher m = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(resposta);
        m.find();
        String token = m.group(1);

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void tokenInvalidoDevolve401() throws Exception {
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer isto.nao.e-um-token"))
                .andExpect(status().isUnauthorized());
    }
}
