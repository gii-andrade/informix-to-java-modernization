package br.com.pedidos.relatorio.dto;

import br.com.pedidos.pedido.enums.StatusPedido;

import java.math.BigDecimal;

/**
 * DTO de saída do relatório de pedidos agrupados por status.
 *
 * Espelha exatamente as colunas do relatório legado relatorio_pedidos_status()
 * do relatorio.4gl (linhas 13-14):
 *
 *   SELECT status, COUNT(*), SUM(valor_total)
 *     FROM pedido GROUP BY status ORDER BY status
 *
 * O legado exibia os dados em janela de terminal; a API retorna JSON.
 */
public record RelatorioPedidosStatusDTO(

    /**
     * Status do pedido — espelha a coluna {@code status} da tabela {@code pedido}.
     * Equivale a {@code v_status} do cursor {@code c_status} no legado.
     */
    StatusPedido status,

    /**
     * Quantidade de pedidos neste status — equivale a {@code v_quantidade}
     * (COUNT(*) do legado).
     */
    long quantidade,

    /**
     * Soma dos valores totais dos pedidos neste status — equivale a {@code v_valor}
     * (SUM(valor_total) do legado). Zero quando não há pedidos.
     */
    BigDecimal valorTotal
) {}
