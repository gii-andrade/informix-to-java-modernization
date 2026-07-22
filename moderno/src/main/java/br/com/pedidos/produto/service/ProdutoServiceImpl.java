package br.com.pedidos.produto.service;

import br.com.pedidos.produto.dto.ProdutoRequestDTO;
import br.com.pedidos.produto.dto.ProdutoResponseDTO;
import br.com.pedidos.produto.entity.Produto;
import br.com.pedidos.produto.exception.CodigoProdutoDuplicadoException;
import br.com.pedidos.produto.exception.ProdutoNaoEncontradoException;
import br.com.pedidos.produto.exception.ProdutoPossuiItensException;
import br.com.pedidos.produto.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implementação do serviço de produtos.
 *
 * Cada método documenta sua correspondência com o legado produto.4gl e as
 * regras de negócio preservadas ou lacunas corrigidas.
 */
@Service
@RequiredArgsConstructor
public class ProdutoServiceImpl implements ProdutoService {

    private final ProdutoRepository repository;

    // -------------------------------------------------------------------------
    // produto_incluir() — linha 10 do produto.4gl
    //
    // Regras preservadas:
    //   • codigo, descricao, preco_venda, estoque_atual e ativo são obrigatórios
    //     (todos REQUIRED no produto.per)
    //   • ativo inicia como 'S' (LET ativo = 'S', linha 18 do produto.4gl)
    //   • codigo deve ser único (uq_produto_codigo)
    //   • preco_venda >= 0 (ck_produto_preco)
    //   • estoque_atual >= 0 (ck_produto_estoque) — DEFAULT 0 aplicado quando omitido
    //
    // Lacuna corrigida:
    //   • unidade_medida estava no schema mas ausente no .per — API aceita o campo
    //     e aplica o DEFAULT 'UN' do Informix quando não informado.
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public ProdutoResponseDTO incluir(ProdutoRequestDTO dto) {
        if (repository.existsByCodigo(dto.codigo())) {
            throw new CodigoProdutoDuplicadoException(dto.codigo());
        }

        Produto produto = Produto.builder()
            .codigo(dto.codigo())
            .descricao(dto.descricao())
            // Preserva DEFAULT 'UN' do schema quando não informado
            .unidadeMedida(dto.unidadeMedida() != null ? dto.unidadeMedida() : "UN")
            .precoVenda(dto.precoVenda())
            // Preserva DEFAULT 0 do schema quando estoque não informado
            .estoqueAtual(dto.estoqueAtual() != null ? dto.estoqueAtual() : BigDecimal.ZERO)
            // LET ativo = 'S' (produto_incluir, linha 18 do produto.4gl)
            .ativo(dto.ativo() != null ? dto.ativo() : "S")
            .build();

        return ProdutoResponseDTO.from(repository.save(produto));
    }

    // -------------------------------------------------------------------------
    // produto_consultar() — linha 34 do produto.4gl
    //
    // Regra preservada:
    //   • ERROR "Produto nao encontrado." quando SQLCODE <> 0 (linha 45)
    // -------------------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public ProdutoResponseDTO buscarPorId(Long id) {
        return ProdutoResponseDTO.from(
            repository.findById(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id))
        );
    }

    // -------------------------------------------------------------------------
    // Adição: listagem completa (sem correspondente na TUI legada)
    // -------------------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public List<ProdutoResponseDTO> listarTodos() {
        return repository.findAll()
            .stream()
            .map(ProdutoResponseDTO::from)
            .toList();
    }

    // -------------------------------------------------------------------------
    // produto_alterar() — LACUNA CORRIGIDA
    //
    // O legado não tinha esta função (identificado no relatório técnico).
    // A API REST supre essa ausência com PUT /produtos/{id}.
    //
    // Regras aplicadas (consistentes com as demais funções do legado):
    //   • produto deve existir antes de alterar
    //   • codigo mantém unicidade — mas o produto pode manter o próprio código
    //   • preco_venda >= 0 (ck_produto_preco) — validado no DTO
    //   • estoque_atual >= 0 (ck_produto_estoque) — validado no DTO
    //   • ativo só aceita 'S' ou 'N' (ck_produto_ativo) — validado no DTO
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public ProdutoResponseDTO alterar(Long id, ProdutoRequestDTO dto) {
        Produto produto = repository.findById(id)
            .orElseThrow(() -> new ProdutoNaoEncontradoException(id));

        if (repository.existsByCodigoAndIdNot(dto.codigo(), id)) {
            throw new CodigoProdutoDuplicadoException(dto.codigo());
        }

        produto.setCodigo(dto.codigo());
        produto.setDescricao(dto.descricao());
        produto.setUnidadeMedida(dto.unidadeMedida() != null ? dto.unidadeMedida() : produto.getUnidadeMedida());
        produto.setPrecoVenda(dto.precoVenda());
        produto.setEstoqueAtual(dto.estoqueAtual() != null ? dto.estoqueAtual() : produto.getEstoqueAtual());
        produto.setAtivo(dto.ativo() != null ? dto.ativo() : produto.getAtivo());

        return ProdutoResponseDTO.from(repository.save(produto));
    }

    // -------------------------------------------------------------------------
    // Operação especializada: alterar apenas o preço de venda
    //
    // Motivação: o legado não permitia alterar preço em nenhuma tela — qualquer
    // ajuste exigia excluir e recriar o produto. Este endpoint atômico evita
    // re-envio de todos os campos apenas para corrigir o preço.
    //
    // Regra: ck_produto_preco → precoVenda >= 0 (validado no DTO de chamada)
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public ProdutoResponseDTO alterarPreco(Long id, BigDecimal novoPreco) {
        if (novoPreco == null || novoPreco.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Preço de venda não pode ser negativo.");
        }

        Produto produto = repository.findById(id)
            .orElseThrow(() -> new ProdutoNaoEncontradoException(id));

        produto.setPrecoVenda(novoPreco);
        return ProdutoResponseDTO.from(repository.save(produto));
    }

    // -------------------------------------------------------------------------
    // Operação especializada: ajustar estoque
    //
    // Motivação: o legado não oferecia ajuste de estoque via TUI — apenas a
    // inclusão definia o valor inicial. Este endpoint permite correções pontuais.
    //
    // Regra: ck_produto_estoque → estoqueAtual >= 0 (validado aqui)
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public ProdutoResponseDTO ajustarEstoque(Long id, BigDecimal novoEstoque) {
        if (novoEstoque == null || novoEstoque.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Estoque não pode ser negativo.");
        }

        Produto produto = repository.findById(id)
            .orElseThrow(() -> new ProdutoNaoEncontradoException(id));

        produto.setEstoqueAtual(novoEstoque);
        return ProdutoResponseDTO.from(repository.save(produto));
    }

    // -------------------------------------------------------------------------
    // produto_excluir() — linha 55 do produto.4gl
    //
    // Regras preservadas:
    //   • ERROR "Produto possui itens ou nao existe." — dois cenários distintos:
    //     1. produto não existe → ProdutoNaoEncontradoException (404)
    //     2. produto tem itens  → ProdutoPossuiItensException   (409)
    //   • A FK fk_item_pedido_produto continua sendo a guardiã real no banco.
    //     O flush() força o DELETE dentro da transação para que a FK dispare
    //     e a exceção seja capturada antes do commit.
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public void excluir(Long id) {
        if (!repository.existsById(id)) {
            throw new ProdutoNaoEncontradoException(id);
        }
        try {
            repository.deleteById(id);
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ProdutoPossuiItensException(id);
        }
    }
}
