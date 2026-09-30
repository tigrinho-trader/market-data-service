package com.tigrinhotrader.marketdata.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tigrinhotrader.marketdata.dominio.Cotacao;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisCotacaoCacheTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private StringRedisTemplate redis;
    private ValueOperations<String, String> valores;
    private SetOperations<String, String> conjuntos;
    private RedisCotacaoCache cache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valores = mock(ValueOperations.class);
        conjuntos = mock(SetOperations.class);
        when(redis.opsForValue()).thenReturn(valores);
        when(redis.opsForSet()).thenReturn(conjuntos);
        cache = new RedisCotacaoCache(redis, objectMapper);
    }

    private final Cotacao btc = new Cotacao("BTCUSDT", new BigDecimal("65000.1"), new BigDecimal("0.5"),
            Instant.parse("2026-09-30T12:00:00Z"));

    @Test
    void salvaJsonEIndexaSimbolo() throws Exception {
        cache.salvar(btc);
        verify(valores).set("cotacao:BTCUSDT", objectMapper.writeValueAsString(btc));
        verify(conjuntos).add("cotacoes:simbolos", "BTCUSDT");
    }

    @Test
    void buscaEDesserializa() throws Exception {
        when(valores.get("cotacao:BTCUSDT")).thenReturn(objectMapper.writeValueAsString(btc));
        assertThat(cache.buscar("BTCUSDT")).contains(btc);
        assertThat(cache.buscar("ETHUSDT")).isEmpty();
    }

    @Test
    void listaEmOrdemAlfabeticaIgnorandoChavesExpiradas() throws Exception {
        when(conjuntos.members("cotacoes:simbolos")).thenReturn(new LinkedHashSet<>(List.of("ETHUSDT", "BTCUSDT")));
        when(valores.get("cotacao:BTCUSDT")).thenReturn(objectMapper.writeValueAsString(btc));
        assertThat(cache.listar()).containsExactly(btc);
    }

    @Test
    void listaVaziaQuandoIndiceNaoExiste() {
        when(conjuntos.members("cotacoes:simbolos")).thenReturn(null);
        assertThat(cache.listar()).isEmpty();
    }

    @Test
    void jsonCorrompidoGeraErroClaro() {
        when(valores.get("cotacao:BTCUSDT")).thenReturn("{nao-e-json");
        assertThatThrownBy(() -> cache.buscar("BTCUSDT")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void falhaDeSerializacaoGeraErroClaro() throws Exception {
        ObjectMapper quebrado = mock(ObjectMapper.class);
        when(quebrado.writeValueAsString(any())).thenThrow(new JsonProcessingException("x") { });
        RedisCotacaoCache comErro = new RedisCotacaoCache(redis, quebrado);
        assertThatThrownBy(() -> comErro.salvar(btc)).isInstanceOf(IllegalStateException.class);
        verify(valores, org.mockito.Mockito.never()).set(eq("cotacao:BTCUSDT"), any());
    }
}
