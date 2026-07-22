package br.com.pedidos.pedido.repository;

import br.com.pedidos.pedido.entity.ItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório JPA para a entidade {@link ItemPedido}.
 *
 * Substitui as operações de INSERT/DELETE em item_pedido do pedido.4gl.
 * A maioria das operações de item é gerenciada via cascade pela entidade Pedido,
 * mas este repositório é necessário para operações diretas de consulta por item.
 */
@Repository
public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    /**
     * Verifica se um produto já existe como item em um pedido.
     * Preserva a constraint uq_item_pedido (pedido_id, produto_id).
     * No legado o controle era implícito via SQLCODE != 0.
     */
    boolean existsByPedidoIdAndProdutoId(Long pedidoId, Long produtoId);
}
