# market-data-service

Parte do projeto **Tigrinho Trader** (disciplina Projeto de Software).

## Finalidade

Busca e cacheia cotacao de mercado real. Mantem conexao WebSocket com a Binance (cripto) e/ou consulta a brapi.dev (acoes B3). Usa Singleton no gerenciador da conexao externa.

## Como interage com os outros servicos

- Publica (assincrono, via fila): cada atualizacao de preco relevante, consumida pelo `trading-service`.
- Exposto (sincrono, via API Gateway): consulta pontual de cotacao atual, se o frontend precisar exibir direto.

## API (via gateway: `/api/cotacoes`)

| Metodo | Caminho | Descricao |
|---|---|---|
| GET | `/cotacoes` | Ultimo preco de todos os ativos acompanhados |
| GET | `/cotacoes/{simbolo}` | Ultimo preco de um ativo (ex.: `BTCUSDT`), 404 se ainda nao chegou |

Funcionamento:

- `BinanceWebSocketManager` (**Singleton**) abre uma unica conexao no stream combinado `<simbolo>@miniTicker`
  de `wss://data-stream.binance.vision` (endpoint so de dados de mercado, funciona tambem a partir de servidores nos EUA) e reconecta sozinho se cair.
- Cada preco vai pro Redis (`cotacao:{SIMBOLO}`) e, se mudou, e publicado como `preco.atualizado` no exchange `tigrinho.eventos`.
- Sem acesso a Binance, ligue o simulador (`SIMULADOR_ENABLED=true`, `BINANCE_ENABLED=false`).

## Rodando

```bash
mvn verify            # testes + relatorio Jacoco (target/site/jacoco); falha abaixo de 80% de linhas
```

Variaveis: `REDIS_HOST`, `RABBITMQ_HOST`, `MARKET_DATA_SIMBOLOS` (padrao `BTCUSDT,ETHUSDT,SOLUSDT`),
`BINANCE_ENABLED`, `BINANCE_WS_URL`, `SIMULADOR_ENABLED`, `MARKET_DATA_CACHE` (`redis` ou `memoria`).

## Repositorios do projeto

- [trading-service](https://github.com/tigrinho-trader/trading-service)
- [wallet-service](https://github.com/tigrinho-trader/wallet-service)
- [notification-service](https://github.com/tigrinho-trader/notification-service)
- [api-gateway](https://github.com/tigrinho-trader/api-gateway)
- [frontend](https://github.com/tigrinho-trader/frontend)
- [docs-arquitetura](https://github.com/tigrinho-trader/docs-arquitetura)
