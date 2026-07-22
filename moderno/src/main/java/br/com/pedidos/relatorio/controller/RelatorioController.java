package br.com.pedidos.relatorio.controller;

import br.com.pedidos.relatorio.dto.RelatorioPedidosStatusDTO;
import br.com.pedidos.relatorio.service.RelatorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller REST do módulo de Relatórios.
 *
 * Mapeamento da função legada para endpoint HTTP:
 *
 *   relatorio_pedidos_status()  → GET /relatorios/pedidos-por-status  → 200 OK
 *
 * O legado exibia os dados em janela de terminal (OPEN WINDOW relatorio).
 * A API devolve a mesma agregação como JSON, permitindo que qualquer
 * front-end ou cliente REST consuma o relatório.
 */
@RestController
@RequestMapping("/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioService service;

    // -------------------------------------------------------------------------
    // GET /relatorios/pedidos-por-status
    //
    // Porta relatorio_pedidos_status() do relatorio.4gl (linhas 8-27).
    // Retorna lista de { status, quantidade, valorTotal } para cada status
    // presente na base. Lista vazia quando não há pedidos.
    // -------------------------------------------------------------------------
    @GetMapping("/pedidos-por-status")
    public ResponseEntity<List<RelatorioPedidosStatusDTO>> pedidosPorStatus() {
        return ResponseEntity.ok(service.pedidosPorStatus());
    }
}
