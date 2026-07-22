package br.com.pedidos.produto.service;

import br.com.pedidos.produto.dto.ProdutoRequestDTO;
import br.com.pedidos.produto.dto.ProdutoResponseDTO;
import br.com.pedidos.produto.entity.Produto;
import br.com.pedidos.produto.exception.CodigoProdutoDuplicadoException;
import br.com.pedidos.produto.exception.ProdutoNaoEncontradoException;
import br.com.pedidos.produto.exception.ProdutoPossuiItensException;
import br.com.pedidos.produto.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de ProdutoServiceImpl.
 *
 * Cenários cobertos, rastreados ao legado produto.4gl:
 *
 *   produto_incluir():
 *     • incluir_ativoDefault      → LET ativo = 'S' (linha 18)
 *     • incluir_unidadeDefault    → DEFAULT 'UN' do schema quando não informado
 *     • incluir_estoqueDefault    → DEFAULT 0 do schema quando não informado
 *     • incluir_codigoDuplicado   → SQLCODE != 0 (linha 27-28), uq_produto_codigo
 *
 *   produto_consultar():
 *     • buscarPorId_encontrado    → SELECT com sucesso
 *     • buscarPorId_naoEncontrado → ERROR "Produto nao encontrado." (linha 45)
 *
 *   produto_alterar() (lacuna corrigida):
 *     • alterar_sucesso           → atualiza campos
 *     • alterar_codigoDuplicado   → código pertence a outro produto
 *
 *   alterarPreco():
 *     • alterarPreco_sucesso      → atualiza somente precoVenda
 *     • alterarPreco_negativo     → lança IllegalArgumentException
 *
 *   ajustarEstoque():
 *     • ajustarEstoque_sucesso    → atualiza somente estoqueAtual
 *     • ajustarEstoque_negativo   → lança IllegalArgumentException
 *
 *   produto_excluir():
 *     • excluir_sucesso           → DELETE com sucesso
 *     • excluir_naoEncontrado     → ERROR "Produto nao encontrado." (linha 62)
 *     • excluir_comItens          → ERROR "Produto possui itens" — FK fk_item_pedido_produto
 */
@ExtendWith(MockitoExtension.class)
class ProdutoServiceImplTest {

    @Mock
    private ProdutoRepository repository;

    @InjectMocks
    private ProdutoServiceImpl service;

    private Produto produtoSalvo;

    @BeforeEach
    void setUp() {
        produtoSalvo = Produto.builder()
            .id(1L)
            .codigo("NOTE-PRO-14")
            .descricao("Notebook Pro 14 polegadas")
            .unidadeMedida("UN")
            .precoVenda(new BigDecimal("4899.90"))
            .estoqueAtual(new BigDecimal("18.000"))
            .ativo("S")
            .build();
    }

    // -------------------------------------------------------------------------
    // produto_incluir()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("incluir: ativo deve ser 'S' por padrão — LET ativo = 'S' (linha 18 do produto.4gl)")
    void incluir_ativoDefault() {
        ProdutoRequestDTO dto = new ProdutoRequestDTO(
            "NOTE-PRO-14", "Notebook Pro", null,
            new BigDecimal("4899.90"), null, null
        );
        when(repository.existsByCodigo("NOTE-PRO-14")).thenReturn(false);
        when(repository.save(any())).thenReturn(produtoSalvo);

        ProdutoResponseDTO resp = service.incluir(dto);

        assertThat(resp.ativo()).isEqualTo("S");
        verify(repository).save(argThat(p -> "S".equals(p.getAtivo())));
    }

    @Test
    @DisplayName("incluir: unidadeMedida deve ser 'UN' por padrão — DEFAULT 'UN' do schema Informix")
    void incluir_unidadeDefault() {
        ProdutoRequestDTO dto = new ProdutoRequestDTO(
            "NOTE-PRO-14", "Notebook Pro", null,
            new BigDecimal("4899.90"), null, "S"
        );
        when(repository.existsByCodigo("NOTE-PRO-14")).thenReturn(false);
        when(repository.save(any())).thenReturn(produtoSalvo);

        service.incluir(dto);

        verify(repository).save(argThat(p -> "UN".equals(p.getUnidadeMedida())));
    }

    @Test
    @DisplayName("incluir: estoqueAtual deve ser 0 por padrão — DEFAULT 0 do schema Informix")
    void incluir_estoqueDefault() {
        ProdutoRequestDTO dto = new ProdutoRequestDTO(
            "NOTE-PRO-14", "Notebook Pro", null,
            new BigDecimal("4899.90"), null, "S"
        );
        when(repository.existsByCodigo("NOTE-PRO-14")).thenReturn(false);
        when(repository.save(any())).thenReturn(produtoSalvo);

        service.incluir(dto);

        verify(repository).save(argThat(p -> BigDecimal.ZERO.compareTo(p.getEstoqueAtual()) == 0));
    }

    @Test
    @DisplayName("incluir: deve lançar CodigoProdutoDuplicadoException — uq_produto_codigo / SQLCODE != 0")
    void incluir_codigoDuplicado() {
        ProdutoRequestDTO dto = new ProdutoRequestDTO(
            "NOTE-PRO-14", "Outro Notebook", "UN",
            new BigDecimal("3999.00"), BigDecimal.ZERO, "S"
        );
        when(repository.existsByCodigo("NOTE-PRO-14")).thenReturn(true);

        assertThatThrownBy(() -> service.incluir(dto))
            .isInstanceOf(CodigoProdutoDuplicadoException.class)
            .hasMessageContaining("NOTE-PRO-14");

        verify(repository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // produto_consultar()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("buscarPorId: deve retornar produto existente")
    void buscarPorId_encontrado() {
        when(repository.findById(1L)).thenReturn(Optional.of(produtoSalvo));

        ProdutoResponseDTO resp = service.buscarPorId(1L);

        assertThat(resp.id()).isEqualTo(1L);
        assertThat(resp.codigo()).isEqualTo("NOTE-PRO-14");
        assertThat(resp.precoVenda()).isEqualByComparingTo("4899.90");
    }

    @Test
    @DisplayName("buscarPorId: deve lançar ProdutoNaoEncontradoException — ERROR 'Produto nao encontrado.'")
    void buscarPorId_naoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
            .isInstanceOf(ProdutoNaoEncontradoException.class)
            .hasMessageContaining("99");
    }

    // -------------------------------------------------------------------------
    // produto_alterar() — lacuna corrigida
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("alterar: deve atualizar todos os campos do produto")
    void alterar_sucesso() {
        ProdutoRequestDTO dto = new ProdutoRequestDTO(
            "NOTE-PRO-14", "Notebook Pro Atualizado", "UN",
            new BigDecimal("4599.90"), new BigDecimal("20.000"), "N"
        );
        when(repository.findById(1L)).thenReturn(Optional.of(produtoSalvo));
        when(repository.existsByCodigoAndIdNot("NOTE-PRO-14", 1L)).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProdutoResponseDTO resp = service.alterar(1L, dto);

        assertThat(resp.descricao()).isEqualTo("Notebook Pro Atualizado");
        assertThat(resp.precoVenda()).isEqualByComparingTo("4599.90");
        assertThat(resp.ativo()).isEqualTo("N");
    }

    @Test
    @DisplayName("alterar: deve lançar CodigoProdutoDuplicadoException se código pertence a outro produto")
    void alterar_codigoDuplicado() {
        ProdutoRequestDTO dto = new ProdutoRequestDTO(
            "MON-27-QHD", "Notebook renomeado", "UN",
            new BigDecimal("4899.90"), new BigDecimal("18.000"), "S"
        );
        when(repository.findById(1L)).thenReturn(Optional.of(produtoSalvo));
        when(repository.existsByCodigoAndIdNot("MON-27-QHD", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.alterar(1L, dto))
            .isInstanceOf(CodigoProdutoDuplicadoException.class)
            .hasMessageContaining("MON-27-QHD");
    }

    // -------------------------------------------------------------------------
    // alterarPreco()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("alterarPreco: deve atualizar somente o preço de venda")
    void alterarPreco_sucesso() {
        when(repository.findById(1L)).thenReturn(Optional.of(produtoSalvo));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProdutoResponseDTO resp = service.alterarPreco(1L, new BigDecimal("3999.00"));

        assertThat(resp.precoVenda()).isEqualByComparingTo("3999.00");
    }

    @Test
    @DisplayName("alterarPreco: deve lançar IllegalArgumentException para preço negativo — ck_produto_preco")
    void alterarPreco_negativo() {
        assertThatThrownBy(() -> service.alterarPreco(1L, new BigDecimal("-1.00")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("negativo");
    }

    // -------------------------------------------------------------------------
    // ajustarEstoque()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ajustarEstoque: deve atualizar somente o estoque atual")
    void ajustarEstoque_sucesso() {
        when(repository.findById(1L)).thenReturn(Optional.of(produtoSalvo));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProdutoResponseDTO resp = service.ajustarEstoque(1L, new BigDecimal("50.000"));

        assertThat(resp.estoqueAtual()).isEqualByComparingTo("50.000");
    }

    @Test
    @DisplayName("ajustarEstoque: deve lançar IllegalArgumentException para estoque negativo — ck_produto_estoque")
    void ajustarEstoque_negativo() {
        assertThatThrownBy(() -> service.ajustarEstoque(1L, new BigDecimal("-5.000")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("negativo");
    }

    // -------------------------------------------------------------------------
    // produto_excluir()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("excluir: deve remover produto sem itens com sucesso")
    void excluir_sucesso() {
        when(repository.existsById(1L)).thenReturn(true);
        doNothing().when(repository).deleteById(1L);

        assertThatCode(() -> service.excluir(1L)).doesNotThrowAnyException();
        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("excluir: deve lançar ProdutoNaoEncontradoException para ID inexistente")
    void excluir_naoEncontrado() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.excluir(99L))
            .isInstanceOf(ProdutoNaoEncontradoException.class)
            .hasMessageContaining("99");
    }

    @Test
    @DisplayName("excluir: deve lançar ProdutoPossuiItensException quando FK fk_item_pedido_produto é violada")
    void excluir_comItens() {
        when(repository.existsById(1L)).thenReturn(true);
        doThrow(DataIntegrityViolationException.class).when(repository).deleteById(1L);

        assertThatThrownBy(() -> service.excluir(1L))
            .isInstanceOf(ProdutoPossuiItensException.class)
            .hasMessageContaining("1");
    }
}
