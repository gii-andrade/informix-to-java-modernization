package br.com.pedidos.pedido.exception;

/**
 * Lançada quando se tenta adicionar ao pedido um produto inativo ou inexistente.
 *
 * Equivale ao erro do legado:
 *   ERROR "Produto inexistente ou inativo."
 *   (pedido_incluir_itens — linha 51 do pedido.4gl)
 *   — originado quando SELECT preco_venda retornava SQLCODE <> 0
 *   (produto_id inválido ou ativo <> 'S')
 */
public class ProdutoInativoOuInexistenteException extends RuntimeException {

    public ProdutoInativoOuInexistenteException(Long produtoId) {
        super("Produto inexistente ou inativo: id=" + produtoId);
    }
}
