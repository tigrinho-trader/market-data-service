package com.tigrinhotrader.marketdata.cache;

import com.tigrinhotrader.marketdata.dominio.Cotacao;
import java.util.List;
import java.util.Optional;

public interface CotacaoCache {

    void salvar(Cotacao cotacao);

    Optional<Cotacao> buscar(String simbolo);

    List<Cotacao> listar();
}
