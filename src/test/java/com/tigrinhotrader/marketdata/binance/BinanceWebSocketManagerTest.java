package com.tigrinhotrader.marketdata.binance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tigrinhotrader.marketdata.dominio.Cotacao;
import java.net.URI;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BinanceWebSocketManagerTest {

    private final BinanceWebSocketManager manager = BinanceWebSocketManager.getInstance();
    private final URI uri = URI.create("wss://exemplo/stream?streams=btcusdt@miniTicker");
    private final List<Cotacao> recebidas = new CopyOnWriteArrayList<>();
    private final List<WebSocket.Listener> ouvintes = new CopyOnWriteArrayList<>();
    private final AtomicInteger tentativas = new AtomicInteger();
    private WebSocket webSocket;

    private final Function<String, Optional<Cotacao>> parser =
            msg -> Optional.of(new Cotacao(msg, java.math.BigDecimal.ONE, java.math.BigDecimal.ZERO, null));

    @BeforeEach
    void setUp() {
        webSocket = mock(WebSocket.class);
        when(webSocket.sendClose(anyInt(), anyString())).thenReturn(CompletableFuture.completedFuture(webSocket));
        manager.configurarParaTeste((u, ouvinte) -> {
            tentativas.incrementAndGet();
            ouvintes.add(ouvinte);
            return CompletableFuture.completedFuture(webSocket);
        }, 10);
    }

    @AfterEach
    void tearDown() {
        manager.desconectar();
    }

    @Test
    void eSingleton() {
        assertThat(BinanceWebSocketManager.getInstance()).isSameAs(manager);
    }

    @Test
    void montaUriDoStreamCombinado() {
        assertThat(BinanceWebSocketManager.montarUri("wss://data-stream.binance.vision/", List.of("BTCUSDT", "ETHUSDT")))
                .hasToString("wss://data-stream.binance.vision/stream?streams=btcusdt@miniTicker/ethusdt@miniTicker");
    }

    @Test
    void conectaUmaVezSoEEntregaMensagensFragmentadas() {
        manager.conectar(uri, parser, recebidas::add);
        manager.conectar(uri, parser, recebidas::add);

        assertThat(tentativas).hasValue(1);
        assertThat(manager.isConectado()).isTrue();

        WebSocket.Listener ouvinte = ouvintes.get(0);
        ouvinte.onOpen(webSocket);
        ouvinte.onText(webSocket, "BTC", false);
        ouvinte.onText(webSocket, "USDT", true);

        assertThat(recebidas).extracting(Cotacao::simbolo).containsExactly("BTCUSDT");
    }

    @Test
    void erroNoProcessamentoNaoDerrubaConexao() {
        manager.conectar(uri, msg -> {
            throw new IllegalStateException("boom");
        }, recebidas::add);
        ouvintes.get(0).onText(webSocket, "x", true);
        assertThat(recebidas).isEmpty();
        assertThat(manager.isConectado()).isTrue();
    }

    @Test
    void reconectaQuandoServidorFechaOuDaErro() {
        manager.conectar(uri, parser, recebidas::add);
        ouvintes.get(0).onClose(webSocket, 1001, "manutencao");
        esperarTentativas(2);
        ouvintes.get(1).onError(webSocket, new RuntimeException("rede"));
        esperarTentativas(3);
    }

    @Test
    void reconectaQuandoConexaoInicialFalha() {
        AtomicInteger chamadas = new AtomicInteger();
        manager.configurarParaTeste((u, ouvinte) -> {
            tentativas.incrementAndGet();
            return chamadas.incrementAndGet() == 1
                    ? CompletableFuture.failedFuture(new RuntimeException("451"))
                    : CompletableFuture.completedFuture(webSocket);
        }, 10);
        manager.conectar(uri, parser, recebidas::add);
        esperarTentativas(2);
        assertThat(manager.isConectado()).isTrue();
    }

    @Test
    void desconectarFechaSocketENaoReconecta() {
        manager.conectar(uri, parser, recebidas::add);
        WebSocket.Listener ouvinte = ouvintes.get(0);
        manager.desconectar();

        verify(webSocket).sendClose(WebSocket.NORMAL_CLOSURE, "encerrando");
        assertThat(manager.isConectado()).isFalse();

        ouvinte.onClose(webSocket, 1000, "ok");
        manager.desconectar();
        assertThat(tentativas).hasValue(1);
    }

    private void esperarTentativas(int esperado) {
        long limite = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (tentativas.get() < esperado && System.nanoTime() < limite) {
            Thread.onSpinWait();
        }
        assertThat(tentativas.get()).isGreaterThanOrEqualTo(esperado);
    }
}
