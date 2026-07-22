package br.com.pedidos.pedido.entity;

import br.com.pedidos.produto.entity.Produto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidade que espelha a tabela {@code item_pedido} do legado Informix.
 *
 * Mapeamento fiel ao schema.sql:
 *   - item_pedido_id SERIAL        PK
 *   - pedido_id      INTEGER       NOT NULL  FK → pedido   (fk_item_pedido_pedido)
 *   - produto_id     INTEGER       NOT NULL  FK → produto  (fk_item_pedido_produto)
 *   - quantidade     DECIMAL(12,3) NOT NULL  → ck_item_quantidade: > 0
 *   - preco_unitario DECIMAL(12,2) NOT NULL  → ck_item_preco: >= 0
 *   - desconto       DECIMAL(12,2) NOT NULL  DEFAULT 0  → ck_item_desconto: >= 0
 *   - valor_total    DECIMAL(12,2) NOT NULL  → ck_item_total: >= 0
 *
 * Constraint uq_item_pedido: um produto não pode aparecer duas vezes no mesmo pedido.
 *
 * Regra de cálculo do legado (pedido_incluir_itens, linha 54):
 *   LET v_total = v_quantidade * v_preco
 *   (desconto sempre 0 na inclusão — campo estrutural não operacional no legado)
 *
 * Na API o cálculo é: valor_total = (quantidade × preco_unitario) - desconto
 * O campo desconto agora é operacional (lacuna corrigida).
 */
@Entity
@Table(
    name = "item_pedido",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_item_pedido", columnNames = {"pedido_id", "produto_id"}
    ),
    indexes = @Index(name = "ix_item_pedido_pedido", columnList = "pedido_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_pedido_id")
    private Long id;

    /**
     * Pedido ao qual este item pertence.
     * Lado "many" do relacionamento bidirecional com Pedido.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_item_pedido_pedido"))
    private Pedido pedido;

    /**
     * Produto referenciado — FK fk_item_pedido_produto.
     * O legado validava: WHERE produto_id = v_produto_id AND ativo = 'S'
     * Produto inativo é rejeitado na inclusão de item (RN preservada no service).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_item_pedido_produto"))
    private Produto produto;

    /**
     * Quantidade — ck_item_quantidade: quantidade > 0.
     * DECIMAL(12,3): suporta frações para produtos vendidos por peso/volume.
     */
    @Column(name = "quantidade", precision = 12, scale = 3, nullable = false)
    private BigDecimal quantidade;

    /**
     * Preço unitário capturado do produto no momento da inclusão do item.
     * Equivale ao SELECT preco_venda do legado — congelado no momento da venda,
     * independente de alterações futuras no cadastro do produto.
     * ck_item_preco: preco_unitario >= 0.
     */
    @Column(name = "preco_unitario", precision = 12, scale = 2, nullable = false)
    private BigDecimal precoUnitario;

    /**
     * Desconto aplicado ao item — DEFAULT 0 no schema.
     * ck_item_desconto: desconto >= 0.
     *
     * Lacuna corrigida: no legado o desconto era sempre gravado como 0
     * (pedido_incluir_itens, linha 57: VALUES (..., 0, v_total)).
     * A API REST torna este campo operacional.
     */
    @Column(name = "desconto", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal desconto = BigDecimal.ZERO;

    /**
     * Valor total do item: (quantidade × preco_unitario) - desconto.
     * ck_item_total: valor_total >= 0.
     *
     * No legado: LET v_total = v_quantidade * v_preco (sem desconto aplicado).
     * Calculado e atualizado pelo service — nunca enviado pelo cliente da API.
     */
    @Column(name = "valor_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal valorTotal;

    // -------------------------------------------------------------------------
    // Método de domínio
    // -------------------------------------------------------------------------

    /**
     * Recalcula e atualiza valorTotal com base nos campos atuais.
     *
     * Fórmula: (quantidade × precoUnitario) - desconto
     * Equivale a: LET v_total = v_quantidade * v_preco (legado, linha 54)
     * Acrescenta: subtração do desconto (lacuna corrigida).
     */
    public void recalcularValorTotal() {
        BigDecimal bruto = quantidade.multiply(precoUnitario);
        this.valorTotal = bruto.subtract(desconto).max(BigDecimal.ZERO);
    }
}
