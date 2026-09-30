package com.tigrinhotrader.marketdata.binance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigrinhotrader.marketdata.config.MarketDataProperties;
import com.tigrinhotrader.marketdata.servico.CotacaoService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

/** Liga o singleton da Binance ao ciclo de vida do Spring. */
@Configuration
public class BinanceConfig {

    private final MarketDataProperties propriedades;
    private final CotacaoService cotacaoService;
    private final BinanceMensagemParser parser;

    public BinanceConfig(MarketDataProperties propriedades, CotacaoService cotacaoService, ObjectMapper objectMapper) {
        this.propriedades = propriedades;
        this.cotacaoService = cotacaoService;
        this.parser = new BinanceMensagemParser(objectMapper);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void iniciar() {
        if (propriedades.binance().enabled()) {
            BinanceWebSocketManager.getInstance().conectar(
                    BinanceWebSocketManager.montarUri(propriedades.binance().url(), propriedades.simbolos()),
                    parser::parse,
                    cotacaoService::registrar);
        }
    }

    @EventListener(ContextClosedEvent.class)
    public void encerrar() {
        BinanceWebSocketManager.getInstance().desconectar();
    }
}
