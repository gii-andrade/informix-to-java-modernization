package br.com.pedidos.pedido.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

/**
 * DTO de entrada para criação de pedido.
 *
 * Validações mapeadas a partir do legado pedido_incluir():
 *   - clienteId  → campo REQUIRED no pedido.per (v_cliente = pedido.cliente_id, REQUIRED)
 *   - usuarioId  → campo REQUIRED no pedido.per (v_usuario = pedido.usuario_id, REQUIRED)
 *   - observacao → opcional no pedido.per e no schema (nullable)
 *   - itens      → pelo menos 1 item obrigatório (o legado entrava em loop até ter ao menos 1)
 *
 * status e valorTotal NÃO são aceitos do cliente:
 *   - status é sempre 'ABERTO' na criação (DEFAULT do schema, sem INPUT no pedido.per)
 *   - valorTotal é calculado pelo service (pedido_recalcular_total)
 */
@Builder
public record PedidoRequestDTO(

    @NotNull(message = "ID do cliente é obrigatório")
    Long clienteId,

    @NotNull(message = "ID do usuário é obrigatório")
    Long usuarioId,

    @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres")
    String observacao,

    /**
     * Lista de itens — obrigatória e não vazia.
     * O legado adicionava itens em loop separado (pedido_incluir_itens),
     * mas sempre exigia pelo menos um item antes de fechar.
     * A API unifica cabeçalho + itens em uma única requisição atômica.
     */
    @NotEmpty(message = "O pedido deve conter pelo menos um item")
    @Valid
    List<ItemPedidoRequestDTO> itens

) {}
