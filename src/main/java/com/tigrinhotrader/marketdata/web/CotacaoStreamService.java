package com.tigrinhotrader.marketdata.web;

import com.tigrinhotrader.marketdata.dominio.Cotacao;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Distribui a mesma cotacao recebida da Binance para todos os navegadores conectados. */
@Service
public class CotacaoStreamService {

    private final List<SseEmitter> clientes = new CopyOnWriteArrayList<>();

    public SseEmitter assinar(List<Cotacao> atuais) {
        SseEmitter emitter = new SseEmitter(0L);
        clientes.add(emitter);
        Runnable remover = () -> clientes.remove(emitter);
        emitter.onCompletion(remover);
        emitter.onTimeout(remover);
        emitter.onError(e -> remover.run());
        try {
            for (Cotacao cotacao : atuais) {
                emitter.send(SseEmitter.event().name("cotacao").data(cotacao));
            }
        } catch (IOException e) {
            remover.run();
            emitter.completeWithError(e);
        }
        return emitter;
    }

    public void publicar(Cotacao cotacao) {
        for (SseEmitter emitter : clientes) {
            try {
                emitter.send(SseEmitter.event().name("cotacao").data(cotacao));
            } catch (IOException | IllegalStateException e) {
                clientes.remove(emitter);
                emitter.complete();
            }
        }
    }
}
