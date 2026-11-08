package br.com.playspot.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.playspot.usuario.UsuarioRepository;

/** Cada teste roda numa transação que é desfeita no fim, então não deixa dados no banco. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CadastroControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarios;

    private static String json(String email, String senha, String papel) {
        return """
                {"nome":"Maria","email":"%s","senha":"%s","telefone":"47999990000","papel":"%s"}
                """.formatted(email, senha, papel);
    }

    @Test
    void cadastraUsuarioEGuardaSenhaComoHash() throws Exception {
        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Maria@Exemplo.com", "senha-segura-123", "JOGADOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("maria@exemplo.com"))
                .andExpect(jsonPath("$.papel").value("JOGADOR"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        var salvo = usuarios.findByEmail("maria@exemplo.com").orElseThrow();
        assertThat(salvo.getSenhaHash()).isNotEqualTo("senha-segura-123").startsWith("$2");
    }

    @Test
    void recusaEmailRepetido() throws Exception {
        String corpo = json("repetido@exemplo.com", "senha-segura-123", "DONO");

        mockMvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Este e-mail já está cadastrado."));
    }

    @Test
    void recusaSenhaCurta() throws Exception {
        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("curta@exemplo.com", "123", "JOGADOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Confira os dados informados."))
                .andExpect(jsonPath("$.campos.senha").value("A senha deve ter entre 8 e 72 caracteres."));
    }

    @Test
    void recusaCadastroComoAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("admin@exemplo.com", "senha-segura-123", "ADMIN")))
                .andExpect(status().isBadRequest());
    }
}
