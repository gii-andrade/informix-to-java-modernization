package br.com.pedidos.pedido.dto;

import br.com.pedidos.pedido.entity.ItemPedido;

import java.math.BigDecimal;

/**
 * DTO de saída para um item de pedido.
 *
 * Expõe os campos calculados (precoUnitario, desconto, valorTotal) que no legado
 * ficavam apenas no banco — a TUI não os exibia para o operador.
 */
public record ItemPedidoResponseDTO(
    Long id,
    Long produtoId,
    String codigoProduto,
    String descricaoProduto,
    BigDecimal quantidade,
    BigDecimal precoUnitario,
    BigDecimal desconto,
    BigDecimal valorTotal
) {

    /** Converte uma entidade {@link ItemPedido} para o DTO de resposta. */
    public static ItemPedidoResponseDTO from(ItemPedido item) {
        return new ItemPedidoResponseDTO(
            item.getId(),
            item.getProduto().getId(),
            item.getProduto().getCodigo(),
            item.getProduto().getDescricao(),
            item.getQuantidade(),
            item.getPrecoUnitario(),
            item.getDesconto(),
            item.getValorTotal()
        );
    }
}
