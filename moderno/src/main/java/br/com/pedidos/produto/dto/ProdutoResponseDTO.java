package br.com.pedidos.produto.dto;

import br.com.pedidos.produto.entity.Produto;

import java.math.BigDecimal;

/**
 * DTO de saída retornado nas respostas da API.
 *
 * Expõe todos os campos da tabela {@code produto}, incluindo {@code unidade_medida}
 * que o formulário legado produto.per omitia — dado existente no banco mas
 * inacessível pela TUI original.
 */
public record ProdutoResponseDTO(
    Long id,
    String codigo,
    String descricao,
    String unidadeMedida,
    BigDecimal precoVenda,
    BigDecimal estoqueAtual,
    String ativo
) {

    /** Converte uma entidade {@link Produto} para o DTO de resposta. */
    public static ProdutoResponseDTO from(Produto produto) {
        return new ProdutoResponseDTO(
            produto.getId(),
            produto.getCodigo(),
            produto.getDescricao(),
            produto.getUnidadeMedida(),
            produto.getPrecoVenda(),
            produto.getEstoqueAtual(),
            produto.getAtivo()
        );
    }
}
