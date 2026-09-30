package com.tigrinhotrader.marketdata.binance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigrinhotrader.marketdata.dominio.Cotacao;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;

/**
 * Converte mensagens do stream combinado de miniTicker da Binance:
 * {"stream":"btcusdt@miniTicker","data":{"s":"BTCUSDT","c":"...","o":"...","E":...}}.
 */
public class BinanceMensagemParser {

    private final ObjectMapper objectMapper;

    public BinanceMensagemParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<Cotacao> parse(String mensagem) {
        try {
            JsonNode raiz = objectMapper.readTree(mensagem);
            JsonNode dados = raiz.has("data") ? raiz.get("data") : raiz;
            if (!dados.hasNonNull("s") || !dados.hasNonNull("c")) {
                return Optional.empty();
            }
            BigDecimal fechamento = new BigDecimal(dados.get("c").asText());
            BigDecimal abertura = dados.hasNonNull("o") ? new BigDecimal(dados.get("o").asText()) : BigDecimal.ZERO;
            Instant quando = dados.hasNonNull("E") ? Instant.ofEpochMilli(dados.get("E").asLong()) : Instant.now();
            return Optional.of(new Cotacao(dados.get("s").asText(), fechamento, variacao(abertura, fechamento), quando));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    static BigDecimal variacao(BigDecimal abertura, BigDecimal fechamento) {
        if (abertura.signum() == 0) {
            return BigDecimal.ZERO.setScale(4);
        }
        return fechamento.subtract(abertura)
                .multiply(BigDecimal.valueOf(100))
                .divide(abertura, 4, RoundingMode.HALF_UP);
    }
}
