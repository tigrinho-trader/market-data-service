package com.tigrinhotrader.marketdata.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tigrinhotrader.marketdata.cache.MemoriaCotacaoCache;
import com.tigrinhotrader.marketdata.dominio.Cotacao;
import com.tigrinhotrader.marketdata.mensageria.CotacaoPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CotacaoServiceTest {

    private MemoriaCotacaoCache cache;
    private CotacaoPublisher publisher;
    private CotacaoService service;

    @BeforeEach
    void setUp() {
        cache = new MemoriaCotacaoCache();
        publisher = mock(CotacaoPublisher.class);
        service = new CotacaoService(cache, publisher);
    }

    private static Cotacao cotacao(String simbolo, String preco) {
        return new Cotacao(simbolo, new BigDecimal(preco), BigDecimal.ZERO, Instant.now());
    }

    @Test
    void primeiraCotacaoSempreEPublicada() {
        assertThat(service.registrar(cotacao("BTCUSDT", "100"))).isTrue();
        verify(publisher).publicar(any());
    }

    @Test
    void precoIgualNaoRepublica() {
        service.registrar(cotacao("BTCUSDT", "100.00"));
        assertThat(service.registrar(cotacao("BTCUSDT", "100.0"))).isFalse();
        verify(publisher, times(1)).publicar(any());
    }

    @Test
    void precoDiferenteRepublicaEAtualizaCache() {
        service.registrar(cotacao("BTCUSDT", "100"));
        assertThat(service.registrar(cotacao("BTCUSDT", "101"))).isTrue();
        verify(publisher, times(2)).publicar(any());
        assertThat(service.buscar("btcusdt")).get().extracting(Cotacao::preco).isEqualTo(new BigDecimal("101"));
    }

    @Test
    void listarDelegaParaCache() {
        service.registrar(cotacao("ETHUSDT", "1"));
        service.registrar(cotacao("BTCUSDT", "2"));
        assertThat(service.listar()).extracting(Cotacao::simbolo).containsExactly("BTCUSDT", "ETHUSDT");
        assertThat(service.buscar("SOLUSDT")).isEmpty();
        verify(publisher, never()).publicar(cotacao("SOLUSDT", "1"));
    }
}
