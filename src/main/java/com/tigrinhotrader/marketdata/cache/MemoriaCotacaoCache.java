package com.tigrinhotrader.marketdata.cache;

import com.tigrinhotrader.marketdata.dominio.Cotacao;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Cache em memoria, usado em testes e para rodar local sem Redis (market-data.cache=memoria). */
@Component
@ConditionalOnProperty(name = "market-data.cache", havingValue = "memoria")
public class MemoriaCotacaoCache implements CotacaoCache {

    private final Map<String, Cotacao> cotacoes = new ConcurrentHashMap<>();

    @Override
    public void salvar(Cotacao cotacao) {
        cotacoes.put(cotacao.simbolo(), cotacao);
    }

    @Override
    public Optional<Cotacao> buscar(String simbolo) {
        return Optional.ofNullable(cotacoes.get(simbolo));
    }

    @Override
    public List<Cotacao> listar() {
        return cotacoes.values().stream().sorted(Comparator.comparing(Cotacao::simbolo)).toList();
    }
}
