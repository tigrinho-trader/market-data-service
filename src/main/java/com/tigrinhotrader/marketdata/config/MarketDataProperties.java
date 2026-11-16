package com.tigrinhotrader.marketdata.config;

import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Propriedades mutáveis para o binder do Spring Boot no Compose/EasyPanel. */
@ConfigurationProperties(prefix = "market-data")
public class MarketDataProperties {
    private List<String> simbolos = List.of("BTCUSDT", "ETHUSDT");
    private Binance binance = new Binance(false, null);
    private Deriv deriv = new Deriv(false, null);

    public MarketDataProperties() {
    }

    public MarketDataProperties(List<String> simbolos, Binance binance) {
        this(simbolos, binance, null);
    }

    public MarketDataProperties(List<String> simbolos, Binance binance, Deriv deriv) {
        setSimbolos(simbolos);
        setBinance(binance);
        setDeriv(deriv);
    }

    public List<String> simbolos() { return simbolos; }

    public void setSimbolos(List<String> simbolos) {
        this.simbolos = simbolos == null || simbolos.isEmpty()
                ? List.of("BTCUSDT", "ETHUSDT")
                : simbolos.stream().map(s -> s.trim().toUpperCase(Locale.ROOT)).toList();
    }

    /**
     * Simbolo da Deriv (indices sinteticos 1HZ... e R_..., forex e metais frx...): vai pela conexao da Deriv, nao pela
     * Binance.
     */
    public static boolean daDeriv(String simbolo) {
        String s = simbolo == null ? "" : simbolo.trim().toUpperCase(Locale.ROOT);
        return s.startsWith("1HZ") || s.startsWith("R_") || s.startsWith("FRX");
    }

    /** O nome que a Deriv aceita: o prefixo frx em minusculas (frxXAUUSD); o resto em maiusculas. */
    public static String nomeNaDeriv(String simbolo) {
        String s = simbolo.trim().toUpperCase(Locale.ROOT);
        return s.startsWith("FRX") ? "frx" + s.substring(3) : s;
    }

    public Binance binance() { return binance; }
    public void setBinance(Binance binance) { this.binance = binance == null ? new Binance(false, null) : binance; }
    public Deriv deriv() { return deriv; }
    public void setDeriv(Deriv deriv) { this.deriv = deriv == null ? new Deriv(false, null) : deriv; }

    public static class Binance {
        private boolean enabled;
        private String url = "wss://data-stream.binance.vision";
        public Binance() { }
        public Binance(boolean enabled, String url) { this.enabled = enabled; setUrl(url); }
        public boolean enabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String url() { return url; }
        public void setUrl(String url) { this.url = url == null || url.isBlank() ? "wss://data-stream.binance.vision" : url; }
    }

    public static class Deriv {
        private boolean enabled;
        private String url = "wss://api.derivws.com/trading/v1/options/ws/public";
        public Deriv() { }
        public Deriv(boolean enabled, String url) { this.enabled = enabled; setUrl(url); }
        public boolean enabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String url() { return url; }
        public void setUrl(String url) { this.url = url == null || url.isBlank() ? "wss://api.derivws.com/trading/v1/options/ws/public" : url; }
    }
}
