package br.com.pedidos.produto.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidade que espelha a tabela {@code produto} do legado Informix.
 *
 * Mapeamento fiel ao schema.sql:
 *   - produto_id     SERIAL       PK
 *   - codigo         VARCHAR(30)  NOT NULL UNIQUE  → uq_produto_codigo
 *   - descricao      VARCHAR(150) NOT NULL
 *   - unidade_medida VARCHAR(10)  NOT NULL DEFAULT 'UN'  (campo ausente no .per, presente no schema)
 *   - preco_venda    DECIMAL(12,2) NOT NULL         → ck_produto_preco: >= 0
 *   - estoque_atual  DECIMAL(12,3) NOT NULL DEFAULT 0  → ck_produto_estoque: >= 0
 *   - ativo          CHAR(1)      NOT NULL DEFAULT 'S'  → ck_produto_ativo: 'S' ou 'N'
 *
 * Lacunas do legado corrigidas:
 *   • produto_alterar() não existia no .4gl — a API REST supre essa ausência com PUT /produtos/{id}.
 *   • unidade_medida existia na tabela mas era invisível no produto.per — agora exposta pela API.
 *   • preco_venda e estoque_atual usam BigDecimal para precisão decimal exata (vs. DECIMAL(12,x) Informix).
 */
@Entity
@Table(
    name = "produto",
    uniqueConstraints = @UniqueConstraint(name = "uq_produto_codigo", columnNames = "codigo")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "produto_id")
    private Long id;

    /**
     * Código único do produto — campo REQUIRED no produto.per.
     * Constraint uq_produto_codigo no schema.
     */
    @Column(name = "codigo", length = 30, nullable = false)
    private String codigo;

    /** Descrição obrigatória — campo REQUIRED no produto.per. */
    @Column(name = "descricao", length = 150, nullable = false)
    private String descricao;

    /**
     * Unidade de medida — presente no schema (DEFAULT 'UN') mas omitida no produto.per.
     * Exposta pela API REST para não perder dados existentes no banco.
     */
    @Column(name = "unidade_medida", length = 10, nullable = false)
    @Builder.Default
    private String unidadeMedida = "UN";

    /**
     * Preço de venda — campo REQUIRED no produto.per.
     * ck_produto_preco: preco_venda >= 0.
     * BigDecimal garante precisão equivalente ao DECIMAL(12,2) do Informix.
     */
    @Column(name = "preco_venda", precision = 12, scale = 2, nullable = false)
    private BigDecimal precoVenda;

    /**
     * Estoque atual — campo REQUIRED no produto.per.
     * ck_produto_estoque: estoque_atual >= 0.
     * DEFAULT 0 no schema → @Builder.Default = ZERO.
     * DECIMAL(12,3): três casas decimais para produtos vendidos por peso/volume.
     */
    @Column(name = "estoque_atual", precision = 12, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal estoqueAtual = BigDecimal.ZERO;

    /**
     * Flag de ativação — campo REQUIRED no produto.per.
     * ck_produto_ativo: apenas 'S' ou 'N'.
     * Novo produto começa ativo ('S') — equivalente ao LET ativo = 'S' da função produto_incluir().
     * Produto inativo não pode ser adicionado a pedidos (validado em pedido_incluir_itens()).
     */
    @Column(name = "ativo", length = 1, nullable = false)
    @Builder.Default
    private String ativo = "S";
}
