package br.com.pedidos.pedido.exception;

import br.com.pedidos.pedido.enums.StatusPedido;

/**
 * Lançada quando se tenta realizar uma transição de status inválida.
 *
 * Melhoria em relação ao legado: pedido_cancelar() fazia UPDATE direto sem
 * validar o status anterior — pedidos FATURADOS podiam ser cancelados.
 * A API impõe a máquina de estados do enum StatusPedido.
 *
 * Exemplos:
 *   Cancelar pedido FATURADO  → 409 Conflict
 *   Faturar pedido CANCELADO  → 409 Conflict
 */
public class TransicaoStatusInvalidaException extends RuntimeException {

    public TransicaoStatusInvalidaException(Long pedidoId,
                                            StatusPedido atual,
                                            String operacao) {
        super(String.format(
            "Pedido id=%d com status '%s' não pode ser %s.",
            pedidoId, atual.name(), operacao
        ));
    }
}
