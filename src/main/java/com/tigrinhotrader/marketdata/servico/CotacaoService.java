package com.tigrinhotrader.marketdata.servico;

import com.tigrinhotrader.marketdata.cache.CotacaoCache;
import com.tigrinhotrader.marketdata.dominio.Cotacao;
import com.tigrinhotrader.marketdata.mensageria.CotacaoPublisher;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CotacaoService {

    private final CotacaoCache cache;
    private final CotacaoPublisher publisher;

    public CotacaoService(CotacaoCache cache, CotacaoPublisher publisher) {
        this.cache = cache;
        this.publisher = publisher;
    }

    /**
     * Guarda a cotacao e publica preco.atualizado so quando o preco mudou
     * (a "atualizacao relevante" da arquitetura), evitando evento repetido.
     */
    public boolean registrar(Cotacao cotacao) {
        Optional<Cotacao> anterior = cache.buscar(cotacao.simbolo());
        cache.salvar(cotacao);
        boolean mudou = anterior.map(a -> a.preco().compareTo(cotacao.preco()) != 0).orElse(true);
        if (mudou) {
            publisher.publicar(cotacao);
        }
        return mudou;
    }

    public Optional<Cotacao> buscar(String simbolo) {
        return cache.buscar(simbolo.toUpperCase(Locale.ROOT));
    }

    public List<Cotacao> listar() {
        return cache.listar();
    }
}
