package com.tigrinhotrader.marketdata.binance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigrinhotrader.marketdata.dominio.Cotacao;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BinanceMensagemParserTest {

    private final BinanceMensagemParser parser = new BinanceMensagemParser(new ObjectMapper());

    @Test
    void parseStreamCombinadoDeMiniTicker() {
        String msg = """
                {"stream":"btcusdt@miniTicker","data":{"e":"24hrMiniTicker","E":1727700000000,
                "s":"BTCUSDT","c":"66000.00","o":"60000.00","h":"1","l":"1","v":"1","q":"1"}}
                """;
        Cotacao c = parser.parse(msg).orElseThrow();
        assertThat(c.simbolo()).isEqualTo("BTCUSDT");
        assertThat(c.preco()).isEqualByComparingTo("66000");
        assertThat(c.variacaoPercentual24h()).isEqualByComparingTo("10");
        assertThat(c.atualizadoEm()).isEqualTo(Instant.ofEpochMilli(1727700000000L));
    }

    @Test
    void parseMensagemSemEnvelopeESemAberturaNemHorario() {
        Cotacao c = parser.parse("{\"s\":\"ETHUSDT\",\"c\":\"3000.5\"}").orElseThrow();
        assertThat(c.preco()).isEqualByComparingTo("3000.5");
        assertThat(c.variacaoPercentual24h()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(c.atualizadoEm()).isNotNull();
    }

    @Test
    void ignoraMensagensSemPrecoOuInvalidas() {
        assertThat(parser.parse("{\"result\":null,\"id\":1}")).isEmpty();
        assertThat(parser.parse("{\"data\":{\"s\":\"BTCUSDT\"}}")).isEmpty();
        assertThat(parser.parse("nao e json")).isEmpty();
        assertThat(parser.parse("{\"s\":\"BTCUSDT\",\"c\":\"abc\"}")).isEmpty();
    }

    @Test
    void variacaoNegativa() {
        assertThat(BinanceMensagemParser.variacao(new BigDecimal("200"), new BigDecimal("150")))
                .isEqualByComparingTo("-25");
    }
}
