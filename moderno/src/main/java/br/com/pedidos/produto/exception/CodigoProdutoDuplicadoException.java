package br.com.pedidos.produto.exception;

/**
 * Lançada quando se tenta incluir ou alterar um produto com código
 * já cadastrado para outro registro.
 *
 * Equivale ao erro do legado:
 *   ERROR "Erro ao incluir produto."  (produto_incluir — linha 28 do produto.4gl)
 *   — gerado quando SQLCODE != 0 por violação de uq_produto_codigo.
 *
 * Lacuna corrigida: o legado não tinha produto_alterar(), então esse cenário
 * de duplicidade na alteração não existia. A API REST cobre esse caso.
 */
public class CodigoProdutoDuplicadoException extends RuntimeException {

    public CodigoProdutoDuplicadoException(String codigo) {
        super("Já existe um produto cadastrado com o código: " + codigo);
    }
}
