package br.com.playspot.horario;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.playspot.horario.GeradorDeHorarios.Faixa;
import br.com.playspot.horario.GeradorDeHorarios.Slot;

class GeradorDeHorariosTests {

    private static int h(int horas) {
        return horas * 60;
    }

    private static Faixa faixa(int de, int ate, String preco) {
        return new Faixa(h(de), h(ate), new BigDecimal(preco));
    }

    @Test
    void fatiaAFaixaNaDuracaoDaQuadra() {
        assertThat(GeradorDeHorarios.gerar(List.of(faixa(8, 10, "80.00")), 60))
                .containsExactly(new Slot(h(8), h(9), new BigDecimal("80.00")), new Slot(h(9), h(10), new BigDecimal("80.00")));
    }

    @Test
    void precoProporcionalParaDuracaoDiferenteDeUmaHora() {
        assertThat(GeradorDeHorarios.gerar(List.of(faixa(8, 10, "80.00")), 90))
                .containsExactly(new Slot(h(8), h(8) + 90, new BigDecimal("120.00")));
    }

    @Test
    void horarioQueCruzaDuasFaixasSomaCadaParte() {
        List<Slot> slots = GeradorDeHorarios.gerar(List.of(faixa(17, 18, "80.00"), faixa(18, 19, "120.00")), 120);
        assertThat(slots).containsExactly(new Slot(h(17), h(19), new BigDecimal("200.00")));
    }

    @Test
    void naoCriaHorarioNoIntervaloEntreFaixasSeparadas() {
        List<Slot> slots = GeradorDeHorarios.gerar(List.of(faixa(8, 9, "80.00"), faixa(10, 11, "80.00")), 120);
        assertThat(slots).isEmpty();
    }

    @Test
    void sobraMenorQueADuracaoNaoViraHorario() {
        assertThat(GeradorDeHorarios.gerar(List.of(new Faixa(h(8), h(9) + 30, new BigDecimal("60.00"))), 60)).hasSize(1);
    }

    @Test
    void faixaAteMeiaNoiteGeraUltimoHorarioDoDia() {
        List<Slot> slots = GeradorDeHorarios.gerar(List.of(faixa(23, 24, "100.00")), 60);
        assertThat(slots).containsExactly(new Slot(h(23), GeradorDeHorarios.MINUTOS_NO_DIA, new BigDecimal("100.00")));
    }

    @Test
    void ordemDasFaixasNaoImporta() {
        assertThat(GeradorDeHorarios.gerar(List.of(faixa(18, 19, "120.00"), faixa(17, 18, "80.00")), 60))
                .extracting(Slot::preco)
                .containsExactly(new BigDecimal("80.00"), new BigDecimal("120.00"));
    }
}
