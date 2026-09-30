package com.tigrinhotrader.marketdata.binance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigrinhotrader.marketdata.config.MarketDataProperties;
import com.tigrinhotrader.marketdata.servico.CotacaoService;
import java.net.URI;
import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BinanceConfigTest {

    private final AtomicReference<URI> uriConectada = new AtomicReference<>();

    @AfterEach
    void tearDown() {
        BinanceWebSocketManager.getInstance().desconectar();
    }

    private BinanceConfig config(boolean habilitado) {
        WebSocket ws = mock(WebSocket.class);
        when(ws.sendClose(anyInt(), anyString())).thenReturn(CompletableFuture.completedFuture(ws));
        BinanceWebSocketManager.getInstance().configurarParaTeste((uri, ouvinte) -> {
            uriConectada.set(uri);
            return CompletableFuture.completedFuture(ws);
        }, 10);
        MarketDataProperties props = new MarketDataProperties(List.of("btcusdt"),
                new MarketDataProperties.Binance(habilitado, "wss://teste"));
        return new BinanceConfig(props, mock(CotacaoService.class), new ObjectMapper());
    }

    @Test
    void conectaNaInicializacaoQuandoHabilitado() {
        BinanceConfig config = config(true);
        config.iniciar();
        assertThat(uriConectada.get()).hasToString("wss://teste/stream?streams=btcusdt@miniTicker");
        assertThat(BinanceWebSocketManager.getInstance().isConectado()).isTrue();

        config.encerrar();
        assertThat(BinanceWebSocketManager.getInstance().isConectado()).isFalse();
    }

    @Test
    void naoConectaQuandoDesabilitado() {
        config(false).iniciar();
        assertThat(uriConectada.get()).isNull();
    }

    @Test
    void propriedadesTemPadroesSensatos() {
        MarketDataProperties padrao = new MarketDataProperties(null, null);
        assertThat(padrao.simbolos()).containsExactly("BTCUSDT", "ETHUSDT");
        assertThat(padrao.binance().enabled()).isFalse();
        assertThat(padrao.binance().url()).isEqualTo("wss://data-stream.binance.vision");
    }
}
