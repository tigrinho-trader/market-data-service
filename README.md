# market-data-service

Parte do projeto **Tigrinho Trader** (disciplina Projeto de Software).

## Finalidade

Busca e cacheia cotacao de mercado real. Mantem conexao WebSocket com a Binance (cripto) e/ou consulta a brapi.dev (acoes B3). Usa Singleton no gerenciador da conexao externa.

## Como interage com os outros servicos

- Publica (assincrono, via fila): cada atualizacao de preco relevante, consumida pelo `trading-service`.
- Exposto (sincrono, via API Gateway): consulta pontual de cotacao atual, se o frontend precisar exibir direto.

## Repositorios do projeto

- [trading-service](https://github.com/tigrinho-trader/trading-service)
- [wallet-service](https://github.com/tigrinho-trader/wallet-service)
- [notification-service](https://github.com/tigrinho-trader/notification-service)
- [api-gateway](https://github.com/tigrinho-trader/api-gateway)
- [frontend](https://github.com/tigrinho-trader/frontend)
- [docs-arquitetura](https://github.com/tigrinho-trader/docs-arquitetura)
