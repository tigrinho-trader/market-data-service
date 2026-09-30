package com.tigrinhotrader.marketdata.mensageria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MensageriaConfig {

    public static final String ROTA_PRECO_ATUALIZADO = "preco.atualizado";

    @Bean
    TopicExchange eventosExchange(@Value("${mensageria.exchange:tigrinho.eventos}") String nome) {
        return new TopicExchange(nome, true, false);
    }

    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
