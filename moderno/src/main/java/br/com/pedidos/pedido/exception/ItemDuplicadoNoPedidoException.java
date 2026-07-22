package br.com.pedidos.pedido.exception;

/**
 * Lançada quando se tenta adicionar ao pedido um produto que já existe na lista de itens.
 *
 * Equivale à constraint uq_item_pedido (pedido_id, produto_id) do schema Informix.
 * No legado o controle era implícito (SQLCODE != 0 por violação de UNIQUE).
 * A API torna o erro explícito e legível antes de tentar o INSERT.
 */
public class ItemDuplicadoNoPedidoException extends RuntimeException {

    public ItemDuplicadoNoPedidoException(Long pedidoId, Long produtoId) {
        super(String.format(
            "Produto id=%d já existe no pedido id=%d.", produtoId, pedidoId
        ));
    }
}
