package br.com.playspot.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import br.com.playspot.comum.RegraDeNegocioException;

/** Sem @Transactional: cada pedido precisa da própria transação, como numa concorrência real. */
@SpringBootTest
class ReservaConcorrenciaTests {

    private static final int PEDIDOS = 12;

    @Autowired
    ReservaService reservaService;

    @Autowired
    JdbcTemplate jdbc;

    long donoId;
    long jogadorId;
    long empresaId;
    long quadraId;
    LocalDate data = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(5);

    @BeforeEach
    void prepara() {
        String insereUsuario = "insert into usuario(nome, email, senha_hash, papel) values (?, ?, 'h', ?) returning id";
        donoId = jdbc.queryForObject(insereUsuario, Long.class, "Dono", "dono-conc@exemplo.com", "DONO");
        jogadorId = jdbc.queryForObject(insereUsuario, Long.class, "Jo", "jo-conc@exemplo.com", "JOGADOR");
        empresaId = jdbc.queryForObject(
                "insert into empresa(dono_id, nome, aprovada) values (?, 'Concorrencia', true) returning id",
                Long.class, donoId);
        quadraId = jdbc.queryForObject(
                "insert into quadra(empresa_id, nome) values (?, 'Q') returning id", Long.class, empresaId);
        jdbc.update("insert into funcionamento(quadra_id, dia_semana, hora_inicio, hora_fim, preco_hora) "
                + "values (?, ?, '18:00', '19:00', 100)", quadraId, (short) (data.getDayOfWeek().getValue() % 7));
    }

    @AfterEach
    void limpa() {
        jdbc.update("delete from reserva where quadra_id = ?", quadraId);
        jdbc.update("delete from funcionamento where quadra_id = ?", quadraId);
        jdbc.update("delete from quadra where id = ?", quadraId);
        jdbc.update("delete from empresa where id = ?", empresaId);
        jdbc.update("delete from usuario where id in (?, ?)", donoId, jogadorId);
    }

    @Test
    void pedidosSimultaneosGeramUmaUnicaReserva() throws Exception {
        Instant inicio = Instant.parse(data + "T21:00:00Z");
        CountDownLatch largada = new CountDownLatch(1);
        List<Callable<Boolean>> tarefas = new ArrayList<>();
        for (int i = 0; i < PEDIDOS; i++) {
            tarefas.add(() -> {
                largada.await();
                try {
                    reservaService.criar(jogadorId, new ReservaRequest(quadraId, inicio));
                    return true;
                } catch (RegraDeNegocioException e) {
                    return false;
                }
            });
        }

        try (ExecutorService executor = Executors.newFixedThreadPool(PEDIDOS)) {
            List<Future<Boolean>> resultados = tarefas.stream().map(executor::submit).toList();
            largada.countDown();
            int aceitos = 0;
            for (Future<Boolean> resultado : resultados) {
                if (resultado.get()) {
                    aceitos++;
                }
            }
            assertThat(aceitos).isEqualTo(1);
        }
        assertThat(jdbc.queryForObject("select count(*) from reserva where quadra_id = ?", Integer.class, quadraId))
                .isEqualTo(1);
    }
}
