package com.tigrinhotrader.marketdata.mensageria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigrinhotrader.marketdata.dominio.Cotacao;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

class CotacaoPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final MensageriaConfig config = new MensageriaConfig();
    private final TopicExchange exchange = config.eventosExchange("tigrinho.eventos");
    private final CotacaoPublisher publisher = new CotacaoPublisher(rabbitTemplate, exchange);
    private final Cotacao cotacao = new Cotacao("BTCUSDT", BigDecimal.TEN, BigDecimal.ZERO, Instant.now());

    @Test
    void publicaNaRotaPrecoAtualizado() {
        assertThat(publisher.publicar(cotacao)).isTrue();
        verify(rabbitTemplate).convertAndSend("tigrinho.eventos", "preco.atualizado", cotacao);
    }

    @Test
    void falhaNaFilaNaoPropagaExcecao() {
        doThrow(new AmqpConnectException(new RuntimeException("sem broker")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        assertThat(publisher.publicar(cotacao)).isFalse();
    }

    @Test
    void configUsaExchangeDuravelEConversorJson() {
        assertThat(exchange.isDurable()).isTrue();
        assertThat(config.jsonMessageConverter(new ObjectMapper())).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}
