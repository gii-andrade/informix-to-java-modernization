package br.com.pedidos.pedido.exception;

/**
 * Lançada quando um pedido não é encontrado pelo ID informado.
 *
 * Equivale ao erro do legado:
 *   ERROR "Pedido nao encontrado."  (pedido_consultar — linha 85 / pedido_cancelar — linha 102)
 */
public class PedidoNaoEncontradoException extends RuntimeException {

    public PedidoNaoEncontradoException(Long id) {
        super("Pedido não encontrado: id=" + id);
    }
}
