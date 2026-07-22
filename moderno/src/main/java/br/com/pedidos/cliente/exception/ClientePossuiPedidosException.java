package br.com.pedidos.cliente.exception;

/**
 * Lançada quando se tenta excluir um cliente que possui pedidos vinculados.
 *
 * Equivale ao erro do legado:
 *   ERROR "Cliente possui pedidos ou nao existe."  (cliente_excluir)
 *   — originado pela violação da FK fk_pedido_cliente no Informix.
 */
public class ClientePossuiPedidosException extends RuntimeException {

    public ClientePossuiPedidosException(Long id) {
        super("O cliente id=" + id + " possui pedidos e não pode ser excluído.");
    }
}
