package com.tigrinhotrader.marketdata.deriv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigrinhotrader.marketdata.config.MarketDataProperties;
import com.tigrinhotrader.marketdata.servico.CotacaoService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

@Configuration
public class DerivConfig {
    private final MarketDataProperties propriedades;
    private final CotacaoService cotacaoService;
    private final ObjectMapper mapper;
    private DerivWebSocketManager manager;

    public DerivConfig(MarketDataProperties propriedades, CotacaoService cotacaoService, ObjectMapper mapper) {
        this.propriedades = propriedades;
        this.cotacaoService = cotacaoService;
        this.mapper = mapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void iniciar() {
        var ativos = propriedades.simbolos().stream().filter("1HZ100V"::equals).toList();
        if (propriedades.deriv().enabled() && !ativos.isEmpty()) {
            manager = new DerivWebSocketManager(mapper, ativos, propriedades.deriv().url(), cotacaoService::registrar);
            manager.iniciar();
        }
    }

    @EventListener(ContextClosedEvent.class)
    public void encerrar() {
        if (manager != null) manager.encerrar();
    }
}
