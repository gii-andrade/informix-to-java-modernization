package br.com.pedidos.pedido.service;

import br.com.pedidos.pedido.dto.ItemPedidoRequestDTO;
import br.com.pedidos.pedido.dto.PedidoRequestDTO;
import br.com.pedidos.pedido.dto.PedidoResponseDTO;
import br.com.pedidos.pedido.enums.StatusPedido;

import java.util.List;

/**
 * Contrato do serviço de pedidos.
 *
 * Operações mapeadas a partir do legado pedido.4gl:
 *   criar       → pedido_incluir() + pedido_incluir_itens() + pedido_recalcular_total()
 *   buscarPorId → pedido_consultar()
 *   cancelar    → pedido_cancelar()
 *   excluir     → pedido_excluir()
 *
 * Operações novas — lacunas e melhorias:
 *   listarTodos        → sem correspondente na TUI
 *   listarPorStatus    → base do relatório relatorio_pedidos_status()
 *   confirmar          → transição ABERTO → CONFIRMADO (status existia no schema mas sem operação)
 *   faturar            → transição CONFIRMADO → FATURADO (idem)
 *   adicionarItem      → permite adicionar item após criação do pedido
 *   removerItem        → permite remover item individual
 */
public interface PedidoService {

    PedidoResponseDTO criar(PedidoRequestDTO dto);

    PedidoResponseDTO buscarPorId(Long id);

    List<PedidoResponseDTO> listarTodos();

    List<PedidoResponseDTO> listarPorStatus(StatusPedido status);

    /** Transição ABERTO → CONFIRMADO (lacuna do legado). */
    PedidoResponseDTO confirmar(Long id);

    /** Transição CONFIRMADO → FATURADO (lacuna do legado). */
    PedidoResponseDTO faturar(Long id);

    /**
     * Transição → CANCELADO.
     * Equivale a pedido_cancelar() do legado, mas com validação de status.
     */
    PedidoResponseDTO cancelar(Long id);

    /**
     * Adiciona item a pedido existente com recálculo do total.
     * No legado: pedido_incluir_itens() era chamado somente na criação.
     */
    PedidoResponseDTO adicionarItem(Long pedidoId, ItemPedidoRequestDTO dto);

    /**
     * Remove item do pedido e recalcula o total.
     * Sem correspondente no legado (exclusão era sempre do pedido inteiro).
     */
    PedidoResponseDTO removerItem(Long pedidoId, Long produtoId);

    /**
     * Exclui pedido e seus itens em transação.
     * Equivale a pedido_excluir() com BEGIN WORK / COMMIT / ROLLBACK WORK.
     */
    void excluir(Long id);
}
