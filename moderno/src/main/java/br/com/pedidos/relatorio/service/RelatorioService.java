package br.com.pedidos.relatorio.service;

import br.com.pedidos.relatorio.dto.RelatorioPedidosStatusDTO;

import java.util.List;

/**
 * Contrato do serviço de relatórios.
 *
 * Porta a única função de consulta do relatorio.4gl legado:
 *   relatorio_pedidos_status() — linhas 8-27
 *
 * A interface permite substituição da implementação e injeção
 * facilitada nos testes.
 */
public interface RelatorioService {

    /**
     * Retorna a contagem e o valor total de pedidos agrupados por status,
     * ordenados alfabeticamente pelo status.
     *
     * Equivale à query do legado (linhas 13-14):
     *   SELECT status, COUNT(*), SUM(valor_total)
     *     FROM pedido GROUP BY status ORDER BY status
     *
     * @return lista de {@link RelatorioPedidosStatusDTO}, nunca {@code null}.
     *         Retorna lista vazia quando não há pedidos.
     */
    List<RelatorioPedidosStatusDTO> pedidosPorStatus();
}
