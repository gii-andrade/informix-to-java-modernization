package br.com.pedidos.pedido.repository;

import br.com.pedidos.pedido.entity.Pedido;
import br.com.pedidos.pedido.enums.StatusPedido;
import br.com.pedidos.relatorio.dto.RelatorioPedidosStatusDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório JPA para a entidade {@link Pedido}.
 *
 * Substitui as queries ESQL de pedido_incluir(), pedido_consultar(),
 * pedido_cancelar() e pedido_excluir() do pedido.4gl.
 */
@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /**
     * Busca pedido por ID carregando cliente e itens em uma única query
     * (evita N+1 ao serializar o PedidoResponseDTO).
     *
     * Equivale ao SELECT da pedido_consultar() mas retorna o pedido completo,
     * incluindo dados do cliente que o legado exibia apenas como ID.
     */
    @Query("""
        SELECT p FROM Pedido p
        JOIN FETCH p.cliente
        LEFT JOIN FETCH p.itens i
        LEFT JOIN FETCH i.produto
        WHERE p.id = :id
    """)
    Optional<Pedido> findByIdComItens(@Param("id") Long id);

    /**
     * Lista pedidos de um cliente específico — sem correspondente na TUI legada.
     */
    List<Pedido> findByClienteId(Long clienteId);

    /**
     * Lista pedidos por status — base do relatório relatorio_pedidos_status().
     */
    List<Pedido> findByStatus(StatusPedido status);

    /**
     * Agrega pedidos por status retornando contagem e soma dos valores totais.
     *
     * Porta diretamente a query do legado relatorio_pedidos_status() (linhas 13-14):
     *   SELECT status, COUNT(*), SUM(valor_total)
     *     FROM pedido GROUP BY status ORDER BY status
     *
     * COALESCE garante que {@code valorTotal} nunca seja {@code null}
     * (equivale ao {@code IF v_total IS NULL THEN LET v_total = 0} de
     * pedido_recalcular_total no legado).
     */
    @Query("""
        SELECT new br.com.pedidos.relatorio.dto.RelatorioPedidosStatusDTO(
            p.status,
            COUNT(p),
            COALESCE(SUM(p.valorTotal), 0)
        )
        FROM Pedido p
        GROUP BY p.status
        ORDER BY p.status
    """)
    List<RelatorioPedidosStatusDTO> relatorioAgrupadoPorStatus();
}
