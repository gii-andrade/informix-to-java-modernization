package br.com.pedidos.produto.exception;

/**
 * Lançada quando um produto não é encontrado pelo ID informado.
 *
 * Equivale ao erro do legado:
 *   ERROR "Produto nao encontrado."  (produto_consultar — linha 45 do produto.4gl)
 */
public class ProdutoNaoEncontradoException extends RuntimeException {

    public ProdutoNaoEncontradoException(Long id) {
        super("Produto não encontrado: id=" + id);
    }
}
