package com.tigrinhotrader.marketdata.config;

import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "market-data")
public record MarketDataProperties(List<String> simbolos, Binance binance) {

    public MarketDataProperties {
        simbolos = simbolos == null || simbolos.isEmpty()
                ? List.of("BTCUSDT", "ETHUSDT")
                : simbolos.stream().map(s -> s.trim().toUpperCase(Locale.ROOT)).toList();
        binance = binance == null ? new Binance(false, null) : binance;
    }

    public record Binance(boolean enabled, String url) {
        public Binance {
            url = url == null || url.isBlank() ? "wss://data-stream.binance.vision" : url;
        }
    }
}
