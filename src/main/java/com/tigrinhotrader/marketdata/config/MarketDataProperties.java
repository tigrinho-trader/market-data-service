package com.tigrinhotrader.marketdata.config;

import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "market-data")
public record MarketDataProperties(List<String> simbolos, Binance binance, Deriv deriv) {

    public MarketDataProperties(List<String> simbolos, Binance binance) {
        this(simbolos, binance, null);
    }

    public MarketDataProperties {
        simbolos = simbolos == null || simbolos.isEmpty()
                ? List.of("BTCUSDT", "ETHUSDT")
                : simbolos.stream().map(s -> s.trim().toUpperCase(Locale.ROOT)).toList();
        binance = binance == null ? new Binance(false, null) : binance;
        deriv = deriv == null ? new Deriv(false, "wss://api.derivws.com/trading/v1/options/ws/public") : deriv;
    }

    public record Binance(boolean enabled, String url) {
        public Binance {
            url = url == null || url.isBlank() ? "wss://data-stream.binance.vision" : url;
        }
    }

    public record Deriv(boolean enabled, String url) {
        public Deriv {
            url = url == null || url.isBlank() ? "wss://api.derivws.com/trading/v1/options/ws/public" : url;
        }
    }
}
