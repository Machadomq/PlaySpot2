package br.com.playspot.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import br.com.playspot.usuario.UsuarioRepository;

/**
 * Sobe o servidor de verdade (porta aleatória). O MockMvc não passa pela rota interna /error,
 * então só um teste assim pega erros de segurança nas respostas 400/409.
 * Como roda em outra thread, não dá para desfazer por transação: limpamos no final.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErrosHttpRealTests {

    private static final String EMAIL = "http-real@exemplo.com";

    @Value("${local.server.port}")
    int porta;

    @Autowired
    UsuarioRepository usuarios;

    private final HttpClient cliente = HttpClient.newHttpClient();

    @AfterEach
    void limpa() {
        usuarios.findByEmail(EMAIL).ifPresent(usuarios::delete);
    }

    private int postCadastro(String corpo) throws Exception {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/api/auth/cadastro"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();
        return cliente.send(pedido, HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    @Test
    void emailRepetidoDevolve409EValidacaoDevolve400() throws Exception {
        String valido = """
                {"nome":"Real","email":"%s","senha":"senha-segura-123","papel":"JOGADOR"}
                """.formatted(EMAIL);
        String senhaCurta = """
                {"nome":"Real","email":"%s","senha":"123","papel":"JOGADOR"}
                """.formatted(EMAIL);

        assertThat(postCadastro(valido)).isEqualTo(201);
        assertThat(postCadastro(valido)).isEqualTo(409);
        assertThat(postCadastro(senhaCurta)).isEqualTo(400);
    }
}
