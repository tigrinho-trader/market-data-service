package com.tigrinhotrader.marketdata.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigrinhotrader.marketdata.dominio.Cotacao;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Guarda so o preco mais recente de cada ativo no Redis (chave cotacao:{SIMBOLO}). */
@Component
@ConditionalOnProperty(name = "market-data.cache", havingValue = "redis", matchIfMissing = true)
public class RedisCotacaoCache implements CotacaoCache {

    static final String PREFIXO = "cotacao:";
    static final String INDICE = "cotacoes:simbolos";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisCotacaoCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void salvar(Cotacao cotacao) {
        try {
            String chave = chave(cotacao.simbolo());
            redis.opsForValue().set(PREFIXO + chave, objectMapper.writeValueAsString(cotacao));
            redis.opsForSet().add(INDICE, chave);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar cotacao " + cotacao.simbolo(), e);
        }
    }

    @Override
    public Optional<Cotacao> buscar(String simbolo) {
        return Optional.ofNullable(redis.opsForValue().get(PREFIXO + chave(simbolo))).map(this::ler);
    }

    /** Chave em maiusculas: frxXAUUSD e FRXXAUUSD sao a mesma cotacao. */
    private static String chave(String simbolo) {
        return simbolo.toUpperCase(java.util.Locale.ROOT);
    }

    @Override
    public List<Cotacao> listar() {
        Set<String> simbolos = redis.opsForSet().members(INDICE);
        if (simbolos == null) {
            return List.of();
        }
        return simbolos.stream().sorted().map(this::buscar).flatMap(Optional::stream).toList();
    }

    private Cotacao ler(String json) {
        try {
            return objectMapper.readValue(json, Cotacao.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cotacao invalida no cache", e);
        }
    }
}
