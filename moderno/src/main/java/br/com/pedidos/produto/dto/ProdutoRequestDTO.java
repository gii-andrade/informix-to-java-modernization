package br.com.pedidos.produto.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * DTO de entrada para criação e atualização de produtos.
 *
 * Validações mapeadas a partir do legado:
 *   - codigo        → campo REQUIRED no produto.per + UNIQUE (uq_produto_codigo)
 *   - descricao     → campo REQUIRED no produto.per + NOT NULL no schema
 *   - unidadeMedida → presente no schema (DEFAULT 'UN'), ausente no .per — opcional na API
 *   - precoVenda    → campo REQUIRED no produto.per + ck_produto_preco: >= 0
 *   - estoqueAtual  → campo REQUIRED no produto.per + ck_produto_estoque: >= 0
 *   - ativo         → campo REQUIRED no produto.per + ck_produto_ativo: 'S' ou 'N'
 */
@Builder
public record ProdutoRequestDTO(

    @NotBlank(message = "Código é obrigatório")
    @Size(max = 30, message = "Código deve ter no máximo 30 caracteres")
    String codigo,

    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 150, message = "Descrição deve ter no máximo 150 caracteres")
    String descricao,

    /**
     * Unidade de medida — ausente no produto.per original mas presente no schema.
     * Quando não informado, o service aplica o default 'UN' (DEFAULT do schema Informix).
     */
    @Size(max = 10, message = "Unidade de medida deve ter no máximo 10 caracteres")
    String unidadeMedida,

    /**
     * ck_produto_preco: preco_venda >= 0.
     * Obrigatório — campo REQUIRED no produto.per.
     */
    @NotNull(message = "Preço de venda é obrigatório")
    @DecimalMin(value = "0.00", message = "Preço de venda não pode ser negativo")
    @Digits(integer = 10, fraction = 2, message = "Preço de venda deve ter no máximo 10 dígitos inteiros e 2 decimais")
    BigDecimal precoVenda,

    /**
     * ck_produto_estoque: estoque_atual >= 0.
     * Obrigatório — campo REQUIRED no produto.per.
     * DEFAULT 0 no schema: quando não informado o service aplica zero.
     */
    @DecimalMin(value = "0.000", message = "Estoque não pode ser negativo")
    @Digits(integer = 9, fraction = 3, message = "Estoque deve ter no máximo 9 dígitos inteiros e 3 decimais")
    BigDecimal estoqueAtual,

    /**
     * ck_produto_ativo: apenas 'S' ou 'N'.
     * Obrigatório — campo REQUIRED no produto.per.
     * Novo produto começa ativo ('S') — equivalente a LET ativo = 'S' na função produto_incluir().
     */
    @Pattern(regexp = "[SN]", message = "Ativo deve ser 'S' (sim) ou 'N' (não)")
    String ativo

) {}
