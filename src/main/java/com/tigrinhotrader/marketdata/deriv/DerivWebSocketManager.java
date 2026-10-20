package com.tigrinhotrader.marketdata.deriv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigrinhotrader.marketdata.dominio.Cotacao;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Uma conexao publica centralizada da Deriv para os ativos de ticks usados pelo jogo. */
public final class DerivWebSocketManager {
    private static final Logger log = LoggerFactory.getLogger(DerivWebSocketManager.class);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "deriv-reconexao");
        t.setDaemon(true);
        return t;
    });
    private final ObjectMapper mapper;
    private final List<String> simbolos;
    private final URI uri;
    private final Consumer<Cotacao> destino;
    private volatile WebSocket socket;
    private volatile boolean ativo;

    public DerivWebSocketManager(ObjectMapper mapper, List<String> simbolos, String url, Consumer<Cotacao> destino) {
        this.mapper = mapper;
        this.simbolos = simbolos;
        this.uri = URI.create(url);
        this.destino = destino;
    }

    public synchronized void iniciar() {
        if (ativo || simbolos.isEmpty()) return;
        ativo = true;
        abrir();
    }

    public synchronized void encerrar() {
        ativo = false;
        WebSocket atual = socket;
        socket = null;
        if (atual != null) atual.sendClose(WebSocket.NORMAL_CLOSURE, "encerrando").exceptionally(e -> null);
    }

    private void abrir() {
        log.info("Conectando na Deriv publica para {}", simbolos);
        HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(uri, new Listener()).whenComplete((ws, erro) -> {
            if (erro != null) {
                log.warn("Falha ao conectar na Deriv: {}", erro.getMessage());
                reconectar();
            } else {
                socket = ws;
            }
        });
    }

    private void reconectar() {
        if (ativo) executor.schedule(this::abrir, 5, TimeUnit.SECONDS);
    }

    private final class Listener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();
        @Override public void onOpen(WebSocket ws) {
            for (String simbolo : simbolos) {
                ws.sendText("{\"ticks\":\"" + simbolo + "\",\"subscribe\":1}", true);
            }
            ws.request(1);
        }
        @Override public CompletionStage<?> onText(WebSocket ws, CharSequence dados, boolean ultimo) {
            buffer.append(dados);
            if (ultimo) {
                try {
                    JsonNode tick = mapper.readTree(buffer.toString()).path("tick");
                    if (tick.hasNonNull("symbol") && tick.hasNonNull("quote")) {
                        destino.accept(new Cotacao(tick.get("symbol").asText(), tick.get("quote").decimalValue(),
                                null, tick.hasNonNull("epoch") ? Instant.ofEpochSecond(tick.get("epoch").asLong()) : Instant.now()));
                    }
                } catch (Exception e) {
                    log.debug("Mensagem Deriv ignorada: {}", e.getMessage());
                } finally {
                    buffer.setLength(0);
                }
            }
            ws.request(1);
            return null;
        }
        @Override public CompletionStage<?> onClose(WebSocket ws, int codigo, String motivo) { socket = null; reconectar(); return null; }
        @Override public void onError(WebSocket ws, Throwable erro) { log.warn("Erro no WebSocket da Deriv: {}", erro.getMessage()); socket = null; reconectar(); }
    }
}
