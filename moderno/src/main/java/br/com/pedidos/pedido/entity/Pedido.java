package br.com.pedidos.pedido.entity;

import br.com.pedidos.cliente.entity.Cliente;
import br.com.pedidos.pedido.enums.StatusPedido;
import br.com.pedidos.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que espelha a tabela {@code pedido} do legado Informix.
 *
 * Mapeamento fiel ao schema.sql:
 *   - pedido_id   SERIAL        PK
 *   - cliente_id  INTEGER       NOT NULL  FK → cliente     (fk_pedido_cliente)
 *   - usuario_id  INTEGER       NOT NULL  FK → usuario     (fk_pedido_usuario)
 *   - data_pedido DATE          NOT NULL  DEFAULT TODAY
 *   - status      VARCHAR(20)   NOT NULL  DEFAULT 'ABERTO' → ck_pedido_status
 *   - valor_total DECIMAL(12,2) NOT NULL  DEFAULT 0        → ck_pedido_total: >= 0
 *   - observacao  VARCHAR(500)  nullable
 *
 * Relacionamentos:
 *   • ManyToOne com Cliente  (obrigatório — campo REQUIRED no pedido.per)
 *   • OneToMany com ItemPedido (cascade ALL, orphanRemoval para gerenciar itens)
 *
 * NOTA: usuario_id agora referencia a entidade Usuario modernizada.
 * FK fk_pedido_usuario validada no banco e no service.
 */
@Entity
@Table(
    name = "pedido",
    indexes = {
        @Index(name = "ix_pedido_cliente",  columnList = "cliente_id"),
        @Index(name = "ix_pedido_usuario",  columnList = "usuario_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pedido_id")
    private Long id;

    /**
     * Cliente que realizou o pedido — campo REQUIRED no pedido.per.
     * FK fk_pedido_cliente: impede exclusão do cliente enquanto houver pedidos.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_pedido_cliente"))
    private Cliente cliente;

    /**
     * Usuário que registrou o pedido — campo REQUIRED no pedido.per.
     * FK fk_pedido_usuario: impede exclusão do usuário enquanto houver pedidos.
     * Substituído de Long para @ManyToOne após modernização do módulo de Usuários.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_pedido_usuario"))
    private Usuario usuario;

    /**
     * Data do pedido — equivalente ao DEFAULT TODAY do Informix.
     * Imutável após o INSERT.
     */
    @Column(name = "data_pedido", nullable = false, updatable = false)
    @Builder.Default
    private LocalDate dataPedido = LocalDate.now();

    /**
     * Ciclo de vida do pedido — DEFAULT 'ABERTO' no schema.
     * ck_pedido_status: ABERTO | CONFIRMADO | FATURADO | CANCELADO.
     * Armazenado como String para compatibilidade direta com a coluna legada.
     *
     * Regra do legado: pedido_cancelar() fazia UPDATE SET status='CANCELADO' sem
     * validar o status anterior — a API impõe máquina de estados via StatusPedido.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private StatusPedido status = StatusPedido.ABERTO;

    /**
     * Valor total calculado pela soma dos itens — DEFAULT 0 no schema.
     * ck_pedido_total: valor_total >= 0.
     * Recalculado pelo service após cada operação de item
     * (equivalente a pedido_recalcular_total() do legado).
     */
    @Column(name = "valor_total", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    /**
     * Observação livre — campo opcional no pedido.per e no schema (nullable).
     */
    @Column(name = "observacao", length = 500)
    private String observacao;

    /**
     * Itens do pedido — cascade ALL garante que itens sejam persistidos/removidos
     * junto com o pedido. orphanRemoval remove itens que saem da lista.
     *
     * Equivale ao par DELETE FROM item_pedido + DELETE FROM pedido dentro de
     * BEGIN WORK / COMMIT WORK em pedido_excluir().
     */
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemPedido> itens = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Métodos de domínio
    // -------------------------------------------------------------------------

    /**
     * Recalcula e atualiza valorTotal somando os valores de todos os itens.
     *
     * Equivale à função pedido_recalcular_total() do legado:
     *   SELECT SUM(valor_total) INTO v_total FROM item_pedido WHERE pedido_id = p_pedido_id
     *   IF v_total IS NULL THEN LET v_total = 0 END IF
     *   UPDATE pedido SET valor_total = v_total WHERE pedido_id = p_pedido_id
     */
    public void recalcularTotal() {
        this.valorTotal = itens.stream()
            .map(ItemPedido::getValorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Adiciona um item ao pedido e recalcula o total imediatamente.
     * Garante a bidirecionalidade do relacionamento.
     */
    public void adicionarItem(ItemPedido item) {
        item.setPedido(this);
        this.itens.add(item);
        recalcularTotal();
    }

    /**
     * Remove um item do pedido pelo produto_id e recalcula o total.
     * Usado na remoção de item individual sem excluir o pedido inteiro.
     */
    public void removerItem(Long produtoId) {
        itens.removeIf(i -> i.getProduto().getId().equals(produtoId));
        recalcularTotal();
    }
}
