package com.tigrinhotrader.marketdata.simulador;

import com.tigrinhotrader.marketdata.config.MarketDataProperties;
import com.tigrinhotrader.marketdata.dominio.Cotacao;
import com.tigrinhotrader.marketdata.servico.CotacaoService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Feed de precos simulado (passeio aleatorio) pra rodar a aplicacao sem acesso a Binance.
 * Ligado com market-data.simulador.enabled=true.
 */
@Component
@ConditionalOnProperty(name = "market-data.simulador.enabled", havingValue = "true")
public class SimuladorCotacoes {

    private static final Map<String, BigDecimal> PRECOS_INICIAIS = Map.of(
            "BTCUSDT", new BigDecimal("60000.00"),
            "ETHUSDT", new BigDecimal("3000.00"),
            "SOLUSDT", new BigDecimal("150.00"));

    private final MarketDataProperties propriedades;
    private final CotacaoService cotacaoService;
    private final Random aleatorio;
    private final Clock relogio;
    private final Map<String, BigDecimal> atuais = new ConcurrentHashMap<>();

    public SimuladorCotacoes(MarketDataProperties propriedades, CotacaoService cotacaoService) {
        this(propriedades, cotacaoService, new Random(), Clock.systemUTC());
    }

    SimuladorCotacoes(MarketDataProperties propriedades, CotacaoService cotacaoService, Random aleatorio, Clock relogio) {
        this.propriedades = propriedades;
        this.cotacaoService = cotacaoService;
        this.aleatorio = aleatorio;
        this.relogio = relogio;
    }

    @Scheduled(fixedRateString = "${market-data.simulador.intervalo-ms:1000}")
    public void gerar() {
        for (String simbolo : propriedades.simbolos()) {
            BigDecimal inicial = PRECOS_INICIAIS.getOrDefault(simbolo, new BigDecimal("100.00"));
            BigDecimal anterior = atuais.getOrDefault(simbolo, inicial);
            // variacao de ate +-0,2% por tick
            BigDecimal fator = BigDecimal.ONE.add(BigDecimal.valueOf((aleatorio.nextDouble() - 0.5) * 0.004));
            BigDecimal novo = anterior.multiply(fator).setScale(2, RoundingMode.HALF_UP);
            atuais.put(simbolo, novo);
            BigDecimal variacao = novo.subtract(inicial).multiply(BigDecimal.valueOf(100))
                    .divide(inicial, 4, RoundingMode.HALF_UP);
            cotacaoService.registrar(new Cotacao(simbolo, novo, variacao, relogio.instant()));
        }
    }
}
