package com.tigrinhotrader.marketdata.binance;

import com.tigrinhotrader.marketdata.dominio.Cotacao;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton: gerenciador unico da conexao WebSocket com a Binance.
 * So pode existir uma conexao aberta por processo, entao o construtor e privado
 * e o acesso e sempre por {@link #getInstance()}.
 */
public final class BinanceWebSocketManager {

    private static final Logger log = LoggerFactory.getLogger(BinanceWebSocketManager.class);
    private static final BinanceWebSocketManager INSTANCIA = new BinanceWebSocketManager();

    /** Abstrai o HttpClient pra permitir teste sem rede. */
    interface Conector {
        CompletableFuture<WebSocket> conectar(URI uri, WebSocket.Listener ouvinte);
    }

    private final ScheduledExecutorService agendador = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "binance-reconexao");
        t.setDaemon(true);
        return t;
    });

    private Conector conector = (uri, ouvinte) ->
            HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(uri, ouvinte);
    private long atrasoReconexaoMs = 5_000;

    private volatile WebSocket webSocket;
    private volatile boolean ativo;
    private URI uri;
    private Function<String, Optional<Cotacao>> parser;
    private Consumer<Cotacao> destino;

    private BinanceWebSocketManager() {
    }

    public static BinanceWebSocketManager getInstance() {
        return INSTANCIA;
    }

    public static URI montarUri(String baseUrl, List<String> simbolos) {
        String streams = simbolos.stream()
                .map(s -> s.toLowerCase(Locale.ROOT) + "@miniTicker")
                .collect(Collectors.joining("/"));
        return URI.create(baseUrl.replaceAll("/+$", "") + "/stream?streams=" + streams);
    }

    public synchronized void conectar(URI uri, Function<String, Optional<Cotacao>> parser, Consumer<Cotacao> destino) {
        if (ativo) {
            log.info("Conexao com a Binance ja esta ativa, ignorando novo pedido");
            return;
        }
        this.uri = uri;
        this.parser = parser;
        this.destino = destino;
        this.ativo = true;
        abrir();
    }

    public synchronized void desconectar() {
        ativo = false;
        WebSocket atual = webSocket;
        webSocket = null;
        if (atual != null) {
            atual.sendClose(WebSocket.NORMAL_CLOSURE, "encerrando").exceptionally(e -> null);
        }
    }

    public boolean isConectado() {
        return ativo && webSocket != null;
    }

    private void abrir() {
        log.info("Conectando na Binance: {}", uri);
        conector.conectar(uri, new Ouvinte()).whenComplete((ws, erro) -> {
            if (erro != null) {
                log.warn("Falha ao conectar na Binance: {}", erro.getMessage());
                agendarReconexao();
            } else {
                webSocket = ws;
            }
        });
    }

    void agendarReconexao() {
        webSocket = null;
        if (!ativo) {
            return;
        }
        agendador.schedule(() -> {
            if (ativo) {
                abrir();
            }
        }, atrasoReconexaoMs, TimeUnit.MILLISECONDS);
    }

    /** Gancho de teste: troca a forma de conectar e o atraso de reconexao. */
    void configurarParaTeste(Conector conector, long atrasoReconexaoMs) {
        this.conector = conector;
        this.atrasoReconexaoMs = atrasoReconexaoMs;
    }

    final class Ouvinte implements WebSocket.Listener {

        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket ws) {
            log.info("Conectado na Binance");
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence dados, boolean ultimo) {
            buffer.append(dados);
            if (ultimo) {
                String mensagem = buffer.toString();
                buffer.setLength(0);
                try {
                    parser.apply(mensagem).ifPresent(destino);
                } catch (RuntimeException e) {
                    log.warn("Erro processando mensagem da Binance: {}", e.getMessage());
                }
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int codigo, String motivo) {
            log.warn("Binance fechou a conexao ({} {}), reconectando", codigo, motivo);
            agendarReconexao();
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable erro) {
            log.warn("Erro no WebSocket da Binance: {}", erro.getMessage());
            agendarReconexao();
        }
    }
}
