package br.com.pedidos.cliente.exception;

/**
 * Lançada quando um cliente não é encontrado pelo ID informado.
 *
 * Equivale ao erro do legado:
 *   ERROR "Cliente nao encontrado."  (cliente_consultar / cliente_alterar)
 */
public class ClienteNaoEncontradoException extends RuntimeException {

    public ClienteNaoEncontradoException(Long id) {
        super("Cliente não encontrado: id=" + id);
    }
}
