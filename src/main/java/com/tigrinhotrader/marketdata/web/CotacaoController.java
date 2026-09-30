package com.tigrinhotrader.marketdata.web;

import com.tigrinhotrader.marketdata.dominio.Cotacao;
import com.tigrinhotrader.marketdata.servico.CotacaoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/cotacoes")
public class CotacaoController {

    private final CotacaoService cotacaoService;

    public CotacaoController(CotacaoService cotacaoService) {
        this.cotacaoService = cotacaoService;
    }

    @GetMapping
    public List<Cotacao> listar() {
        return cotacaoService.listar();
    }

    @GetMapping("/{simbolo}")
    public Cotacao buscar(@PathVariable String simbolo) {
        return cotacaoService.buscar(simbolo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sem cotacao para " + simbolo));
    }
}
