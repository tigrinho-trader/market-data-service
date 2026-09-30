package com.tigrinhotrader.marketdata.simulador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tigrinhotrader.marketdata.config.MarketDataProperties;
import com.tigrinhotrader.marketdata.dominio.Cotacao;
import com.tigrinhotrader.marketdata.servico.CotacaoService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SimuladorCotacoesTest {

    @Test
    void geraPassseioAleatorioProximoDoPrecoAnterior() {
        CotacaoService service = mock(CotacaoService.class);
        MarketDataProperties props = new MarketDataProperties(List.of("BTCUSDT", "XYZUSDT"), null);
        Clock relogio = Clock.fixed(Instant.parse("2026-09-30T12:00:00Z"), ZoneOffset.UTC);
        SimuladorCotacoes simulador = new SimuladorCotacoes(props, service, new Random(42), relogio);

        simulador.gerar();
        simulador.gerar();

        ArgumentCaptor<Cotacao> captor = ArgumentCaptor.forClass(Cotacao.class);
        verify(service, times(4)).registrar(captor.capture());
        List<Cotacao> geradas = captor.getAllValues();

        Cotacao btc = geradas.get(0);
        assertThat(btc.simbolo()).isEqualTo("BTCUSDT");
        assertThat(btc.preco()).isBetween(new BigDecimal("59880"), new BigDecimal("60120"));
        assertThat(btc.atualizadoEm()).isEqualTo(relogio.instant());
        assertThat(geradas.get(1).preco()).isBetween(new BigDecimal("99.8"), new BigDecimal("100.2"));
        // segundo tick parte do preco anterior, nao do inicial
        assertThat(geradas.get(2).preco()).isBetween(
                btc.preco().multiply(new BigDecimal("0.997")), btc.preco().multiply(new BigDecimal("1.003")));
    }

    @Test
    void construtorPublicoFunciona() {
        SimuladorCotacoes simulador = new SimuladorCotacoes(new MarketDataProperties(null, null),
                mock(CotacaoService.class));
        simulador.gerar();
    }
}
