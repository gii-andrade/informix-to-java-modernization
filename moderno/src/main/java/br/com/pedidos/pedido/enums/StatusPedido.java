package br.com.pedidos.pedido.enums;

/**
 * Ciclo de vida de um pedido — espelha a constraint ck_pedido_status do schema Informix:
 *   CHECK (status IN ('ABERTO','CONFIRMADO','FATURADO','CANCELADO'))
 *
 * Transições permitidas pela regra de negócio:
 *
 *   ABERTO ──► CONFIRMADO  (confirmar)
 *   ABERTO ──► CANCELADO   (cancelar — pedido_cancelar() do legado)
 *   CONFIRMADO ──► FATURADO (faturar)
 *   CONFIRMADO ──► CANCELADO (cancelar)
 *
 * FATURADO e CANCELADO são estados finais — sem transição de saída.
 *
 * Observação sobre o legado: pedido_cancelar() fazia UPDATE direto sem
 * validar o status atual. A API REST impõe a máquina de estados explicitamente
 * para evitar cancelamento de pedidos já faturados.
 */
public enum StatusPedido {
    ABERTO,
    CONFIRMADO,
    FATURADO,
    CANCELADO;

    /** Retorna true se este status admite cancelamento. */
    public boolean cancelavel() {
        return this == ABERTO || this == CONFIRMADO;
    }

    /** Retorna true se este status admite confirmação. */
    public boolean confirmavel() {
        return this == ABERTO;
    }

    /** Retorna true se este status admite faturamento. */
    public boolean faturavel() {
        return this == CONFIRMADO;
    }
}
