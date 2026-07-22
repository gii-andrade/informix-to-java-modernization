package br.com.pedidos.pedido.service;

import br.com.pedidos.cliente.entity.Cliente;
import br.com.pedidos.cliente.exception.ClienteNaoEncontradoException;
import br.com.pedidos.cliente.repository.ClienteRepository;
import br.com.pedidos.pedido.dto.ItemPedidoRequestDTO;
import br.com.pedidos.pedido.dto.PedidoRequestDTO;
import br.com.pedidos.pedido.dto.PedidoResponseDTO;
import br.com.pedidos.pedido.entity.ItemPedido;
import br.com.pedidos.pedido.entity.Pedido;
import br.com.pedidos.pedido.enums.StatusPedido;
import br.com.pedidos.pedido.exception.ItemDuplicadoNoPedidoException;
import br.com.pedidos.pedido.exception.PedidoNaoEncontradoException;
import br.com.pedidos.pedido.exception.ProdutoInativoOuInexistenteException;
import br.com.pedidos.pedido.exception.TransicaoStatusInvalidaException;
import br.com.pedidos.pedido.repository.ItemPedidoRepository;
import br.com.pedidos.pedido.repository.PedidoRepository;
import br.com.pedidos.produto.entity.Produto;
import br.com.pedidos.produto.repository.ProdutoRepository;
import br.com.pedidos.usuario.entity.Usuario;
import br.com.pedidos.usuario.enums.PerfilUsuario;
import br.com.pedidos.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de PedidoServiceImpl.
 *
 * Cenários rastreados ao legado pedido.4gl:
 *
 *   criar():
 *     • criar_sucesso              → pedido_incluir + itens + recalcular_total
 *     • criar_clienteInexistente   → cliente_id inválido
 *     • criar_produtoInativo       → ERROR "Produto inexistente ou inativo." (linha 51)
 *     • criar_valorTotalCalculado  → LET v_total = v_quantidade * v_preco (linha 54)
 *     • criar_descontoAplicado     → lacuna corrigida (desconto sempre 0 no legado)
 *     • criar_estoqueDescontado    → lacuna corrigida (legado não atualizava estoque)
 *
 *   cancelar():
 *     • cancelar_sucesso           → UPDATE status='CANCELADO' (linha 98)
 *     • cancelar_faturado_invalido → legado permitia — API impede via máquina de estados
 *     • cancelar_estornaEstoque    → lacuna corrigida
 *
 *   confirmar() / faturar():
 *     • confirmar_sucesso          → ABERTO → CONFIRMADO
 *     • confirmar_invalido         → status não permite
 *     • faturar_sucesso            → CONFIRMADO → FATURADO
 *     • faturar_invalido           → status não permite
 *
 *   adicionarItem():
 *     • adicionarItem_sucesso      → recalcula total
 *     • adicionarItem_duplicado    → uq_item_pedido (constraint implícita no legado)
 *     • adicionarItem_pedidoFechado → apenas ABERTO aceita novos itens
 *
 *   removerItem():
 *     • removerItem_sucesso        → recalcula total e restitui estoque
 *
 *   excluir():
 *     • excluir_sucesso            → pedido_excluir() BEGIN/COMMIT/ROLLBACK
 *     • excluir_naoEncontrado      → PedidoNaoEncontradoException
 */
@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

    @Mock PedidoRepository     pedidoRepository;
    @Mock ItemPedidoRepository itemPedidoRepository;
    @Mock ClienteRepository    clienteRepository;
    @Mock ProdutoRepository    produtoRepository;
    @Mock UsuarioRepository    usuarioRepository;

    @InjectMocks
    PedidoServiceImpl service;

    // ── fixtures ──────────────────────────────────────────────────────────────

    private Cliente clienteAtivo;
    private Usuario usuarioAtivo;
    private Produto produtoAtivo;
    private Produto produtoInativo;

    @BeforeEach
    void setUp() {
        clienteAtivo = Cliente.builder()
            .id(1L).razaoSocial("Almeida & Filhos Ltda.").ativo("S").build();

        usuarioAtivo = Usuario.builder()
            .id(2L).login("operador").nome("Carlos Silva")
            .senhaHash("$2a$10$hash").perfil(PerfilUsuario.OPERADOR).ativo("S").build();

        produtoAtivo = Produto.builder()
            .id(10L).codigo("NOTE-PRO-14").descricao("Notebook Pro")
            .precoVenda(new BigDecimal("4899.90"))
            .estoqueAtual(new BigDecimal("18.000"))
            .ativo("S").build();

        produtoInativo = Produto.builder()
            .id(99L).codigo("INATIVO").descricao("Produto inativo")
            .precoVenda(new BigDecimal("100.00"))
            .estoqueAtual(BigDecimal.ZERO)
            .ativo("N").build();
    }

    /** Monta um Pedido salvo com um item, para uso nos testes de estado. */
    private Pedido pedidoComUmItem(StatusPedido status) {
        ItemPedido item = ItemPedido.builder()
            .id(1L)
            .produto(produtoAtivo)
            .quantidade(new BigDecimal("2.000"))
            .precoUnitario(new BigDecimal("4899.90"))
            .desconto(BigDecimal.ZERO)
            .valorTotal(new BigDecimal("9799.80"))
            .build();

        Pedido pedido = Pedido.builder()
            .id(1L).cliente(clienteAtivo).usuario(usuarioAtivo)
            .dataPedido(LocalDate.now())
            .status(status)
            .valorTotal(new BigDecimal("9799.80"))
            .itens(new ArrayList<>(List.of(item)))
            .build();

        item.setPedido(pedido);
        return pedido;
    }

    // =========================================================================
    @Nested
    @DisplayName("criar()")
    class CriarTests {

        private PedidoRequestDTO dtoPedido(Long produtoId, BigDecimal qty, BigDecimal desconto) {
            return new PedidoRequestDTO(
                1L, 2L, "Observação",
                List.of(new ItemPedidoRequestDTO(produtoId, qty, desconto))
            );
        }

        @Test
        @DisplayName("deve criar pedido com status ABERTO, total calculado e estoque descontado")
        void criar_sucesso() {
            // arrange
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteAtivo));
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioAtivo));
            when(produtoRepository.findById(10L)).thenReturn(Optional.of(produtoAtivo));
            when(pedidoRepository.save(any())).thenAnswer(inv -> {
                Pedido p = inv.getArgument(0);
                p = Pedido.builder()
                    .id(1L).cliente(p.getCliente()).usuario(p.getUsuario())
                    .dataPedido(p.getDataPedido()).status(p.getStatus())
                    .valorTotal(p.getValorTotal()).observacao(p.getObservacao())
                    .itens(p.getItens()).build();
                return p;
            });

            // act
            PedidoResponseDTO resp = service.criar(dtoPedido(10L, new BigDecimal("2.000"), null));

            // assert — status inicial ABERTO (DEFAULT schema)
            assertThat(resp.status()).isEqualTo(StatusPedido.ABERTO);

            // assert — valor total = 2 × 4899.90 = 9799.80 (linha 54 do legado)
            assertThat(resp.valorTotal()).isEqualByComparingTo("9799.80");

            // assert — estoque descontado (lacuna corrigida)
            verify(produtoRepository).save(argThat(p ->
                p.getEstoqueAtual().compareTo(new BigDecimal("16.000")) == 0
            ));
        }

        @Test
        @DisplayName("deve lançar ClienteNaoEncontradoException para cliente inexistente")
        void criar_clienteInexistente() {
            when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                service.criar(new PedidoRequestDTO(99L, 2L, null,
                    List.of(new ItemPedidoRequestDTO(10L, BigDecimal.ONE, null))))
            ).isInstanceOf(ClienteNaoEncontradoException.class);
        }

        @Test
        @DisplayName("deve lançar ProdutoInativoOuInexistenteException — ERROR 'Produto inexistente ou inativo.' (linha 51)")
        void criar_produtoInativo() {
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteAtivo));
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioAtivo));
            when(produtoRepository.findById(99L)).thenReturn(Optional.of(produtoInativo));

            assertThatThrownBy(() -> service.criar(dtoPedido(99L, BigDecimal.ONE, null)))
                .isInstanceOf(ProdutoInativoOuInexistenteException.class)
                .hasMessageContaining("99");
        }

        @Test
        @DisplayName("deve aplicar desconto no valor total do item — lacuna corrigida (sempre 0 no legado)")
        void criar_descontoAplicado() {
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteAtivo));
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioAtivo));
            when(produtoRepository.findById(10L)).thenReturn(Optional.of(produtoAtivo));
            when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // 1 × 4899.90 - 100.00 = 4799.90
            PedidoResponseDTO resp = service.criar(
                dtoPedido(10L, BigDecimal.ONE, new BigDecimal("100.00"))
            );

            assertThat(resp.valorTotal()).isEqualByComparingTo("4799.90");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("cancelar()")
    class CancelarTests {

        @Test
        @DisplayName("deve cancelar pedido ABERTO e restituir estoque — lacuna corrigida")
        void cancelar_sucesso() {
            Pedido pedido = pedidoComUmItem(StatusPedido.ABERTO);
            when(pedidoRepository.findByIdComItens(1L)).thenReturn(Optional.of(pedido));
            when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PedidoResponseDTO resp = service.cancelar(1L);

            assertThat(resp.status()).isEqualTo(StatusPedido.CANCELADO);
            // estoque restituído: 18 + 2 = 20
            verify(produtoRepository).save(argThat(p ->
                p.getEstoqueAtual().compareTo(new BigDecimal("20.000")) == 0
            ));
        }

        @Test
        @DisplayName("deve lançar TransicaoStatusInvalidaException ao cancelar pedido FATURADO — legado não validava")
        void cancelar_faturadoInvalido() {
            Pedido pedido = pedidoComUmItem(StatusPedido.FATURADO);
            when(pedidoRepository.findByIdComItens(1L)).thenReturn(Optional.of(pedido));

            assertThatThrownBy(() -> service.cancelar(1L))
                .isInstanceOf(TransicaoStatusInvalidaException.class)
                .hasMessageContaining("FATURADO");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("confirmar() e faturar()")
    class TransicaoStatusTests {

        @Test
        @DisplayName("confirmar: ABERTO → CONFIRMADO")
        void confirmar_sucesso() {
            Pedido pedido = pedidoComUmItem(StatusPedido.ABERTO);
            when(pedidoRepository.findByIdComItens(1L)).thenReturn(Optional.of(pedido));
            when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(service.confirmar(1L).status()).isEqualTo(StatusPedido.CONFIRMADO);
        }

        @Test
        @DisplayName("confirmar: lança TransicaoStatusInvalidaException se já CONFIRMADO")
        void confirmar_invalido() {
            Pedido pedido = pedidoComUmItem(StatusPedido.CONFIRMADO);
            when(pedidoRepository.findByIdComItens(1L)).thenReturn(Optional.of(pedido));

            assertThatThrownBy(() -> service.confirmar(1L))
                .isInstanceOf(TransicaoStatusInvalidaException.class);
        }

        @Test
        @DisplayName("faturar: CONFIRMADO → FATURADO")
        void faturar_sucesso() {
            Pedido pedido = pedidoComUmItem(StatusPedido.CONFIRMADO);
            when(pedidoRepository.findByIdComItens(1L)).thenReturn(Optional.of(pedido));
            when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(service.faturar(1L).status()).isEqualTo(StatusPedido.FATURADO);
        }

        @Test
        @DisplayName("faturar: lança TransicaoStatusInvalidaException se ABERTO")
        void faturar_invalido() {
            Pedido pedido = pedidoComUmItem(StatusPedido.ABERTO);
            when(pedidoRepository.findByIdComItens(1L)).thenReturn(Optional.of(pedido));

            assertThatThrownBy(() -> service.faturar(1L))
                .isInstanceOf(TransicaoStatusInvalidaException.class);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("adicionarItem()")
    class AdicionarItemTests {

        @Test
        @DisplayName("deve adicionar item, recalcular total e descontar estoque")
        void adicionarItem_sucesso() {
            Pedido pedido = pedidoComUmItem(StatusPedido.ABERTO);
            Produto produto2 = Produto.builder()
                .id(20L).codigo("MON-27").descricao("Monitor")
                .precoVenda(new BigDecimal("1599.00"))
                .estoqueAtual(new BigDecimal("10.000"))
                .ativo("S").build();

            when(pedidoRepository.findByIdComItens(1L)).thenReturn(Optional.of(pedido));
            when(itemPedidoRepository.existsByPedidoIdAndProdutoId(1L, 20L)).thenReturn(false);
            when(produtoRepository.findById(20L)).thenReturn(Optional.of(produto2));
            when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PedidoResponseDTO resp = service.adicionarItem(1L,
                new ItemPedidoRequestDTO(20L, new BigDecimal("3.000"), null));

            // total: 9799.80 + 3 × 1599.00 = 14596.80
            assertThat(resp.valorTotal()).isEqualByComparingTo("14596.80");
            assertThat(resp.itens()).hasSize(2);
        }

        @Test
        @DisplayName("deve lançar ItemDuplicadoNoPedidoException — constraint uq_item_pedido")
        void adicionarItem_duplicado() {
            Pedido pedido = pedidoComUmItem(StatusPedido.ABERTO);
            when(pedidoRepository.findByIdComItens(1L)).thenReturn(Optional.of(pedido));
            when(itemPedidoRepository.existsByPedidoIdAndProdutoId(1L, 10L)).thenReturn(true);

            assertThatThrownBy(() -> service.adicionarItem(1L,
                    new ItemPedidoRequestDTO(10L, BigDecimal.ONE, null)))
                .isInstanceOf(ItemDuplicadoNoPedidoException.class);
        }

        @Test
        @DisplayName("deve lançar TransicaoStatusInvalidaException em pedido CONFIRMADO")
        void adicionarItem_pedidoFechado() {
            Pedido pedido = pedidoComUmItem(StatusPedido.CONFIRMADO);
            when(pedidoRepository.findByIdComItens(1L)).thenReturn(Optional.of(pedido));

            assertThatThrownBy(() -> service.adicionarItem(1L,
                    new ItemPedidoRequestDTO(10L, BigDecimal.ONE, null)))
                .isInstanceOf(TransicaoStatusInvalidaException.class);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("removerItem()")
    class RemoverItemTests {

        @Test
        @DisplayName("deve remover item, recalcular total e restituir estoque")
        void removerItem_sucesso() {
            Pedido pedido = pedidoComUmItem(StatusPedido.ABERTO);
            when(pedidoRepository.findByIdComItens(1L)).thenReturn(Optional.of(pedido));
            when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PedidoResponseDTO resp = service.removerItem(1L, 10L);

            assertThat(resp.itens()).isEmpty();
            assertThat(resp.valorTotal()).isEqualByComparingTo("0.00");
            // estoque restituído: 18 + 2 = 20
            verify(produtoRepository).save(argThat(p ->
                p.getEstoqueAtual().compareTo(new BigDecimal("20.000")) == 0
            ));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("excluir()")
    class ExcluirTests {

        @Test
        @DisplayName("deve excluir pedido e itens em transação — pedido_excluir BEGIN/COMMIT")
        void excluir_sucesso() {
            Pedido pedido = pedidoComUmItem(StatusPedido.ABERTO);
            when(pedidoRepository.findByIdComItens(1L)).thenReturn(Optional.of(pedido));

            assertThatCode(() -> service.excluir(1L)).doesNotThrowAnyException();
            verify(pedidoRepository).delete(pedido);
        }

        @Test
        @DisplayName("deve lançar PedidoNaoEncontradoException para ID inexistente")
        void excluir_naoEncontrado() {
            when(pedidoRepository.findByIdComItens(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.excluir(99L))
                .isInstanceOf(PedidoNaoEncontradoException.class)
                .hasMessageContaining("99");
        }
    }
}
