package br.com.pedidos.pedido.service;

import br.com.pedidos.cliente.entity.Cliente;
import br.com.pedidos.cliente.repository.ClienteRepository;
import br.com.pedidos.cliente.exception.ClienteNaoEncontradoException;
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
import br.com.pedidos.usuario.exception.UsuarioNaoEncontradoException;
import br.com.pedidos.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implementação do serviço de pedidos.
 *
 * Preserva todas as regras de negócio do legado pedido.4gl e corrige as
 * lacunas identificadas no relatório técnico:
 *
 *   RN preservadas:
 *     • Somente produtos com ativo='S' podem ser adicionados (linha 49 do pedido.4gl)
 *     • Preço do item é capturado do cadastro do produto no momento da inclusão (linha 48)
 *     • valor_total do item = quantidade × preco_unitario (linha 54) — acrescido do desconto
 *     • valor_total do pedido = SUM(item.valor_total) (pedido_recalcular_total)
 *     • Cancelamento é mudança de status, não exclui dados (pedido_cancelar)
 *     • Exclusão remove itens e cabeçalho em transação (pedido_excluir BEGIN/COMMIT/ROLLBACK)
 *     • cliente_id e usuario_id são obrigatórios (REQUIRED no pedido.per)
 *
 *   Lacunas corrigidas:
 *     • Transação incompleta: legado inseria cabeçalho FORA de transação antes dos itens
 *       — aqui tudo ocorre dentro de @Transactional atômica
 *     • Desconto sempre 0 no legado — agora operacional
 *     • Cancelamento sem validação de status — API impõe máquina de estados
 *     • Atualização de estoque: o legado NÃO atualizava estoque ao incluir item —
 *       a API desconta estoque_atual ao adicionar e o restitui ao remover/cancelar
 */
@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository       pedidoRepository;
    private final ItemPedidoRepository   itemPedidoRepository;
    private final ClienteRepository      clienteRepository;
    private final ProdutoRepository      produtoRepository;
    private final UsuarioRepository      usuarioRepository;

    // =========================================================================
    // pedido_incluir() + pedido_incluir_itens() + pedido_recalcular_total()
    // linhas 11–36 + 38–61 + 63–71 do pedido.4gl
    //
    // Regras preservadas:
    //   • cliente_id e usuario_id são obrigatórios (REQUIRED no pedido.per)
    //   • status inicial = 'ABERTO' (DEFAULT do schema, sem INPUT no formulário)
    //   • valor_total inicial = 0 (recalculado após inclusão dos itens)
    //   • Somente produtos com ativo='S' aceitos (linha 49)
    //   • Preço congelado no momento da inclusão (linha 48)
    //   • Desconto = 0 na inclusão sem desconto informado (linha 57)
    //   • valor_total do item = quantidade × preco_unitario - desconto
    //   • valor_total do pedido = SUM(itens)
    //
    // Lacunas corrigidas:
    //   • Cabeçalho + itens na MESMA transação @Transactional (legado: INSERT pedido fora de tx)
    //   • Desconto agora é operacional
    //   • Estoque descontado ao incluir item (legado não alterava estoque)
    //   • uq_item_pedido verificada explicitamente antes do INSERT
    // =========================================================================
    @Override
    @Transactional
    public PedidoResponseDTO criar(PedidoRequestDTO dto) {
        Cliente cliente = clienteRepository.findById(dto.clienteId())
            .orElseThrow(() -> new ClienteNaoEncontradoException(dto.clienteId()));

        // Validação de usuário adicionada após modernização do módulo de Usuários
        Usuario usuario = usuarioRepository.findById(dto.usuarioId())
            .orElseThrow(() -> new UsuarioNaoEncontradoException(dto.usuarioId()));

        Pedido pedido = Pedido.builder()
            .cliente(cliente)
            .usuario(usuario)
            .observacao(dto.observacao())
            .build();

        for (ItemPedidoRequestDTO itemDto : dto.itens()) {
            ItemPedido item = construirItem(pedido, itemDto);
            pedido.adicionarItem(item);
            descontarEstoque(item.getProduto(), item.getQuantidade());
        }

        return PedidoResponseDTO.from(pedidoRepository.save(pedido));
    }

    // =========================================================================
    // pedido_consultar() — linha 73 do pedido.4gl
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public PedidoResponseDTO buscarPorId(Long id) {
        return PedidoResponseDTO.from(
            pedidoRepository.findByIdComItens(id)
                .orElseThrow(() -> new PedidoNaoEncontradoException(id))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarTodos() {
        return pedidoRepository.findAll().stream()
            .map(p -> PedidoResponseDTO.from(
                pedidoRepository.findByIdComItens(p.getId()).orElseThrow()
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarPorStatus(StatusPedido status) {
        return pedidoRepository.findByStatus(status).stream()
            .map(p -> PedidoResponseDTO.from(
                pedidoRepository.findByIdComItens(p.getId()).orElseThrow()
            ))
            .toList();
    }

    // =========================================================================
    // Confirmar pedido — ABERTO → CONFIRMADO
    // Status existia no schema mas não havia operação na TUI (lacuna corrigida)
    // =========================================================================
    @Override
    @Transactional
    public PedidoResponseDTO confirmar(Long id) {
        Pedido pedido = carregarPedidoComItens(id);
        if (!pedido.getStatus().confirmavel()) {
            throw new TransicaoStatusInvalidaException(id, pedido.getStatus(), "confirmado");
        }
        pedido.setStatus(StatusPedido.CONFIRMADO);
        return PedidoResponseDTO.from(pedidoRepository.save(pedido));
    }

    // =========================================================================
    // Faturar pedido — CONFIRMADO → FATURADO
    // Status existia no schema mas não havia operação na TUI (lacuna corrigida)
    // =========================================================================
    @Override
    @Transactional
    public PedidoResponseDTO faturar(Long id) {
        Pedido pedido = carregarPedidoComItens(id);
        if (!pedido.getStatus().faturavel()) {
            throw new TransicaoStatusInvalidaException(id, pedido.getStatus(), "faturado");
        }
        pedido.setStatus(StatusPedido.FATURADO);
        return PedidoResponseDTO.from(pedidoRepository.save(pedido));
    }

    // =========================================================================
    // pedido_cancelar() — linha 95 do pedido.4gl
    //
    // Regra preservada:
    //   • UPDATE pedido SET status = 'CANCELADO' (linha 98)
    //   • Dados do pedido NÃO são excluídos (apenas mudança de status)
    //
    // Lacuna corrigida:
    //   • Legado fazia UPDATE direto sem validar status anterior — pedidos
    //     FATURADOS podiam ser cancelados. A API valida via StatusPedido.cancelavel().
    //
    // Melhoria adicional:
    //   • Ao cancelar, o estoque dos itens é restituído
    //     (o legado não atualizava estoque em nenhuma operação de pedido)
    // =========================================================================
    @Override
    @Transactional
    public PedidoResponseDTO cancelar(Long id) {
        Pedido pedido = carregarPedidoComItens(id);
        if (!pedido.getStatus().cancelavel()) {
            throw new TransicaoStatusInvalidaException(id, pedido.getStatus(), "cancelado");
        }
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.getItens().forEach(item ->
            restituirEstoque(item.getProduto(), item.getQuantidade())
        );
        return PedidoResponseDTO.from(pedidoRepository.save(pedido));
    }

    // =========================================================================
    // Adicionar item a pedido existente
    //
    // No legado: pedido_incluir_itens() só era chamado na criação do pedido.
    // A API permite acrescentar itens a pedidos ABERTOS após a criação.
    //
    // Regras preservadas (mesmas de pedido_incluir_itens):
    //   • Produto deve existir e estar ativo
    //   • Produto não pode ser duplicado no pedido (uq_item_pedido)
    //   • Preço capturado do produto no momento da inclusão
    //   • valor_total do pedido recalculado após adição
    //   • Estoque descontado
    // =========================================================================
    @Override
    @Transactional
    public PedidoResponseDTO adicionarItem(Long pedidoId, ItemPedidoRequestDTO dto) {
        Pedido pedido = carregarPedidoComItens(pedidoId);

        if (pedido.getStatus() != StatusPedido.ABERTO) {
            throw new TransicaoStatusInvalidaException(
                pedidoId, pedido.getStatus(), "modificado (apenas pedidos ABERTOS aceitam novos itens)"
            );
        }

        if (itemPedidoRepository.existsByPedidoIdAndProdutoId(pedidoId, dto.produtoId())) {
            throw new ItemDuplicadoNoPedidoException(pedidoId, dto.produtoId());
        }

        ItemPedido item = construirItem(pedido, dto);
        pedido.adicionarItem(item);
        descontarEstoque(item.getProduto(), item.getQuantidade());

        return PedidoResponseDTO.from(pedidoRepository.save(pedido));
    }

    // =========================================================================
    // Remover item do pedido e recalcular total
    // Sem correspondente no legado — lacuna corrigida
    // =========================================================================
    @Override
    @Transactional
    public PedidoResponseDTO removerItem(Long pedidoId, Long produtoId) {
        Pedido pedido = carregarPedidoComItens(pedidoId);

        if (pedido.getStatus() != StatusPedido.ABERTO) {
            throw new TransicaoStatusInvalidaException(
                pedidoId, pedido.getStatus(), "modificado (apenas pedidos ABERTOS permitem remoção de itens)"
            );
        }

        ItemPedido item = pedido.getItens().stream()
            .filter(i -> i.getProduto().getId().equals(produtoId))
            .findFirst()
            .orElseThrow(() -> new ProdutoInativoOuInexistenteException(produtoId));

        restituirEstoque(item.getProduto(), item.getQuantidade());
        pedido.removerItem(produtoId);

        return PedidoResponseDTO.from(pedidoRepository.save(pedido));
    }

    // =========================================================================
    // pedido_excluir() — linha 106 do pedido.4gl
    //
    // Regra preservada:
    //   • DELETE FROM item_pedido WHERE pedido_id = id
    //   • DELETE FROM pedido WHERE pedido_id = id
    //   • Ambos dentro de @Transactional (equivale a BEGIN WORK / COMMIT / ROLLBACK)
    //
    // Simplificação: cascade CascadeType.ALL na entidade Pedido garante que
    // os itens sejam removidos automaticamente com o pedido — sem DELETE manual.
    // =========================================================================
    @Override
    @Transactional
    public void excluir(Long id) {
        Pedido pedido = carregarPedidoComItens(id);
        pedidoRepository.delete(pedido);
    }

    // =========================================================================
    // Métodos privados auxiliares
    // =========================================================================

    /**
     * Carrega pedido com itens e produtos em uma única query.
     * Lança PedidoNaoEncontradoException se não encontrado.
     */
    private Pedido carregarPedidoComItens(Long id) {
        return pedidoRepository.findByIdComItens(id)
            .orElseThrow(() -> new PedidoNaoEncontradoException(id));
    }

    /**
     * Valida produto (ativo e existente), captura o preço atual e constrói o ItemPedido.
     *
     * Equivale ao bloco do legado (pedido_incluir_itens, linhas 48–58):
     *   SELECT preco_venda INTO v_preco FROM produto
     *    WHERE produto_id = v_produto_id AND ativo = 'S'
     *   IF SQLCA.SQLCODE <> 0 THEN ERROR "Produto inexistente ou inativo."
     *   LET v_total = v_quantidade * v_preco
     *   INSERT INTO item_pedido (..., v_preco, 0, v_total)
     */
    private ItemPedido construirItem(Pedido pedido, ItemPedidoRequestDTO dto) {
        Produto produto = produtoRepository.findById(dto.produtoId())
            .filter(p -> "S".equals(p.getAtivo()))
            .orElseThrow(() -> new ProdutoInativoOuInexistenteException(dto.produtoId()));

        BigDecimal desconto = dto.desconto() != null ? dto.desconto() : BigDecimal.ZERO;

        ItemPedido item = ItemPedido.builder()
            .pedido(pedido)
            .produto(produto)
            .quantidade(dto.quantidade())
            .precoUnitario(produto.getPrecoVenda())   // preço congelado no momento do pedido
            .desconto(desconto)
            .valorTotal(BigDecimal.ZERO)              // será calculado pelo método de domínio
            .build();

        item.recalcularValorTotal();
        return item;
    }

    /**
     * Desconta quantidade do estoque_atual do produto.
     *
     * Lacuna corrigida: o legado não atualizava estoque em nenhuma operação de pedido.
     * A API mantém estoque_atual sincronizado com os pedidos.
     *
     * ck_produto_estoque: estoque >= 0 — validado pelo banco como última linha de defesa.
     */
    private void descontarEstoque(Produto produto, BigDecimal quantidade) {
        produto.setEstoqueAtual(produto.getEstoqueAtual().subtract(quantidade));
        produtoRepository.save(produto);
    }

    /**
     * Restitui quantidade ao estoque_atual do produto.
     * Chamado no cancelamento do pedido e na remoção de item.
     */
    private void restituirEstoque(Produto produto, BigDecimal quantidade) {
        produto.setEstoqueAtual(produto.getEstoqueAtual().add(quantidade));
        produtoRepository.save(produto);
    }
}
