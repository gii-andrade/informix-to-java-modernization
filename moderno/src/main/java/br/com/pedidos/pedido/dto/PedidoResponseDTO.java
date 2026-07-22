package br.com.pedidos.pedido.dto;

import br.com.pedidos.pedido.entity.Pedido;
import br.com.pedidos.pedido.enums.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO de saída para pedido com todos os seus itens.
 *
 * Consolida pedido + itens em uma única resposta, eliminando a necessidade de
 * chamadas separadas — melhoria em relação ao legado que exibia apenas o
 * cabeçalho do pedido no formulário pedido.per.
 *
 * Atualizado após modernização do módulo de Usuários:
 *   usuarioId + nomeUsuario agora refletem a entidade Usuario via @ManyToOne.
 */
public record PedidoResponseDTO(
    Long id,
    Long clienteId,
    String razaoSocialCliente,
    Long usuarioId,
    String nomeUsuario,
    LocalDate dataPedido,
    StatusPedido status,
    BigDecimal valorTotal,
    String observacao,
    List<ItemPedidoResponseDTO> itens
) {

    /** Converte uma entidade {@link Pedido} (com itens carregados) para o DTO de resposta. */
    public static PedidoResponseDTO from(Pedido pedido) {
        return new PedidoResponseDTO(
            pedido.getId(),
            pedido.getCliente().getId(),
            pedido.getCliente().getRazaoSocial(),
            pedido.getUsuario().getId(),
            pedido.getUsuario().getNome(),
            pedido.getDataPedido(),
            pedido.getStatus(),
            pedido.getValorTotal(),
            pedido.getObservacao(),
            pedido.getItens().stream()
                .map(ItemPedidoResponseDTO::from)
                .toList()
        );
    }
}
