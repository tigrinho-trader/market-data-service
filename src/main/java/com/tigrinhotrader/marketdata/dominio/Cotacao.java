package com.tigrinhotrader.marketdata.dominio;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Preco mais recente de um ativo. Tambem e o payload do evento preco.atualizado.
 */
public record Cotacao(String simbolo, BigDecimal preco, BigDecimal variacaoPercentual24h, Instant atualizadoEm) {
}
