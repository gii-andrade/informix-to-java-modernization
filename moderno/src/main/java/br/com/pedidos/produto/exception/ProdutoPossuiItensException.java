package br.com.pedidos.produto.exception;

/**
 * Lançada quando se tenta excluir um produto que possui itens de pedido vinculados.
 *
 * Equivale ao erro do legado:
 *   ERROR "Produto possui itens ou nao existe."  (produto_excluir — linha 62 do produto.4gl)
 *   — originado pela violação da FK fk_item_pedido_produto no Informix.
 */
public class ProdutoPossuiItensException extends RuntimeException {

    public ProdutoPossuiItensException(Long id) {
        super("O produto id=" + id + " possui itens de pedido e não pode ser excluído.");
    }
}
