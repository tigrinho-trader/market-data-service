package com.tigrinhotrader.marketdata;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tigrinhotrader.marketdata.dominio.Cotacao;
import com.tigrinhotrader.marketdata.servico.CotacaoService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/** Sobe o contexto inteiro (cache em memoria, fila mockada) e exercita a API REST. */
@SpringBootTest
@AutoConfigureMockMvc
class MarketDataServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CotacaoService cotacaoService;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void registraCotacaoPublicaEventoEExpoeNaApi() throws Exception {
        Cotacao btc = new Cotacao("BTCUSDT", new BigDecimal("65000.50"), new BigDecimal("1.2500"), Instant.now());
        cotacaoService.registrar(btc);
        cotacaoService.registrar(new Cotacao("ETHUSDT", new BigDecimal("3100"), BigDecimal.ZERO, Instant.now()));

        verify(rabbitTemplate, times(2))
                .convertAndSend(eq("tigrinho.eventos"), eq("preco.atualizado"), any(Object.class));

        mockMvc.perform(get("/cotacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].simbolo").value("BTCUSDT"));

        mockMvc.perform(get("/cotacoes/btcusdt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preco").value(65000.50));
    }

    @Test
    void cotacaoInexistenteDevolve404() throws Exception {
        mockMvc.perform(get("/cotacoes/XPTOUSDT")).andExpect(status().isNotFound());
    }
}
