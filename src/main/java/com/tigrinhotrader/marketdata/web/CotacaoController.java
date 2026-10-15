package com.tigrinhotrader.marketdata.web;

import com.tigrinhotrader.marketdata.dominio.Cotacao;
import com.tigrinhotrader.marketdata.servico.CotacaoService;
import java.util.List;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/cotacoes")
public class CotacaoController {

    private final CotacaoService cotacaoService;
    private final CotacaoStreamService streamService;

    public CotacaoController(CotacaoService cotacaoService, CotacaoStreamService streamService) {
        this.cotacaoService = cotacaoService;
        this.streamService = streamService;
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

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@org.springframework.web.bind.annotation.RequestParam(required = false) String simbolos) {
        List<Cotacao> atuais = cotacaoService.listar();
        if (simbolos == null || simbolos.isBlank()) return streamService.assinar(atuais);
        List<String> aceitos = Arrays.stream(simbolos.split(","))
                .map(s -> s.trim().toUpperCase(Locale.ROOT)).filter(s -> !s.isBlank()).toList();
        return streamService.assinar(atuais.stream().filter(c -> aceitos.contains(c.simbolo())).toList());
    }
}
