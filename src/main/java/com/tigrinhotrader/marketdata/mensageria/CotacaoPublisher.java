package com.tigrinhotrader.marketdata.mensageria;

import com.tigrinhotrader.marketdata.dominio.Cotacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class CotacaoPublisher {

    private static final Logger log = LoggerFactory.getLogger(CotacaoPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange exchange;

    public CotacaoPublisher(RabbitTemplate rabbitTemplate, TopicExchange exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    /** Publica preco.atualizado. Falha na fila nao pode derrubar o stream de precos, entao so loga. */
    public boolean publicar(Cotacao cotacao) {
        try {
            rabbitTemplate.convertAndSend(exchange.getName(), MensageriaConfig.ROTA_PRECO_ATUALIZADO, cotacao);
            return true;
        } catch (AmqpException e) {
            log.warn("Nao foi possivel publicar preco de {}: {}", cotacao.simbolo(), e.getMessage());
            return false;
        }
    }
}
