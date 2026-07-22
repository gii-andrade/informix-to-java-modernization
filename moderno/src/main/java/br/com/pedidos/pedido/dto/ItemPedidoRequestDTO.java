package br.com.pedidos.pedido.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * DTO de entrada para um item de pedido.
 *
 * Validações mapeadas a partir do legado pedido_incluir_itens():
 *   - produtoId   → obrigatório (PROMPT "Produto:" FOR v_produto_id)
 *   - quantidade  → obrigatória e > 0 (ck_item_quantidade)
 *   - desconto    → opcional, >= 0 (ck_item_desconto) — lacuna corrigida (sempre 0 no legado)
 *
 * precoUnitario NÃO é aceito do cliente: o service sempre captura o preço
 * atual do produto no momento da inclusão, como fazia o legado:
 *   SELECT preco_venda INTO v_preco FROM produto WHERE produto_id = v_produto_id AND ativo = 'S'
 */
@Builder
public record ItemPedidoRequestDTO(

    @NotNull(message = "ID do produto é obrigatório")
    Long produtoId,

    /**
     * ck_item_quantidade: quantidade > 0.
     * DECIMAL(12,3) — suporta frações (peso/volume).
     */
    @NotNull(message = "Quantidade é obrigatória")
    @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
    @Digits(integer = 9, fraction = 3, message = "Quantidade: máximo 9 inteiros e 3 decimais")
    BigDecimal quantidade,

    /**
     * Desconto em valor — DEFAULT 0 quando não informado.
     * Lacuna corrigida: no legado era sempre 0 (linha 57 do produto.4gl).
     * ck_item_desconto: desconto >= 0.
     */
    @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
    @Digits(integer = 10, fraction = 2, message = "Desconto: máximo 10 inteiros e 2 decimais")
    BigDecimal desconto

) {}
