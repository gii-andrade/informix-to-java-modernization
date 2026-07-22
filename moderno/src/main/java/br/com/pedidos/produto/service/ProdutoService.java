package br.com.pedidos.produto.service;

import br.com.pedidos.produto.dto.ProdutoRequestDTO;
import br.com.pedidos.produto.dto.ProdutoResponseDTO;

import java.util.List;

/**
 * Contrato do serviço de produtos.
 *
 * Operações mapeadas a partir do legado produto.4gl:
 *   incluir      → produto_incluir()
 *   buscarPorId  → produto_consultar()
 *   excluir      → produto_excluir()
 *
 * Operações novas — lacunas do legado corrigidas pela API REST:
 *   listarTodos  → sem correspondente na TUI
 *   alterar      → produto_alterar() não existia no legado (lacuna identificada no relatório técnico)
 */
public interface ProdutoService {

    ProdutoResponseDTO incluir(ProdutoRequestDTO dto);

    ProdutoResponseDTO buscarPorId(Long id);

    List<ProdutoResponseDTO> listarTodos();

    /**
     * Operação nova — supre a lacuna de produto_alterar() identificada no relatório técnico.
     * Permite atualizar preço, estoque, descrição e demais campos sem exclusão/reinserção.
     */
    ProdutoResponseDTO alterar(Long id, ProdutoRequestDTO dto);

    ProdutoResponseDTO alterarPreco(Long id, java.math.BigDecimal novoPreco);

    ProdutoResponseDTO ajustarEstoque(Long id, java.math.BigDecimal novoEstoque);

    void excluir(Long id);
}
