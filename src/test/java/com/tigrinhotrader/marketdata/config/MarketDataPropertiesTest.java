package com.tigrinhotrader.marketdata.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.tigrinhotrader.marketdata.cache.MemoriaCotacaoCache;
import com.tigrinhotrader.marketdata.dominio.Cotacao;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketDataPropertiesTest {

    @Test
    void ouroPrataEV100VaoPelaDerivComONomeQueElaAceita() {
        // a lista do ambiente vira maiusculas; a Deriv quer o prefixo frx em minusculas
        MarketDataProperties p = new MarketDataProperties(List.of("BTCUSDT", "frxXAUUSD", "frxXAGUSD", "1HZ100V"), null);
        assertThat(p.simbolos()).containsExactly("BTCUSDT", "FRXXAUUSD", "FRXXAGUSD", "1HZ100V");
        assertThat(p.simbolos().stream().filter(MarketDataProperties::daDeriv).map(MarketDataProperties::nomeNaDeriv))
                .containsExactly("frxXAUUSD", "frxXAGUSD", "1HZ100V");
        assertThat(MarketDataProperties.daDeriv("BTCUSDT")).isFalse();
        assertThat(MarketDataProperties.daDeriv("PAXGUSDT")).isFalse();
        assertThat(MarketDataProperties.daDeriv("R_100")).isTrue();
    }

    @Test
    void cotacaoDoOuroAchaPeloNomeEmQualquerCaixa() {
        MemoriaCotacaoCache cache = new MemoriaCotacaoCache();
        cache.salvar(new Cotacao("frxXAUUSD", new BigDecimal("3870.12"), null, Instant.EPOCH));
        assertThat(cache.buscar("FRXXAUUSD")).get().extracting(Cotacao::simbolo).isEqualTo("frxXAUUSD");
        assertThat(cache.buscar("frxXAUUSD")).isPresent();
    }
}
