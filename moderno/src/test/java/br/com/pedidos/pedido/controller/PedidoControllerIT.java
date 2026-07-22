package br.com.pedidos.pedido.controller;

import br.com.pedidos.cliente.entity.Cliente;
import br.com.pedidos.cliente.repository.ClienteRepository;
import br.com.pedidos.pedido.dto.ItemPedidoRequestDTO;
import br.com.pedidos.pedido.dto.PedidoRequestDTO;
import br.com.pedidos.pedido.enums.StatusPedido;
import br.com.pedidos.pedido.repository.PedidoRepository;
import br.com.pedidos.produto.entity.Produto;
import br.com.pedidos.produto.repository.ProdutoRepository;
import br.com.pedidos.usuario.entity.Usuario;
import br.com.pedidos.usuario.enums.PerfilUsuario;
import br.com.pedidos.usuario.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração do PedidoController com H2 em memória.
 *
 * Exercita o fluxo completo: Controller → Service → Repository → H2.
 * Cobre todas as operações, incluindo as que suprem lacunas do legado:
 *   confirmar / faturar / adicionarItem / removerItem / máquina de estados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class PedidoControllerIT {

    @Autowired MockMvc             mvc;
    @Autowired ObjectMapper        mapper;
    @Autowired PedidoRepository    pedidoRepository;
    @Autowired ClienteRepository   clienteRepository;
    @Autowired ProdutoRepository   produtoRepository;
    @Autowired UsuarioRepository   usuarioRepository;

    private Long clienteId;
    private Long usuarioId;
    private Long produtoId1;
    private Long produtoId2;

    @BeforeEach
    void setup() {
        pedidoRepository.deleteAll();
        clienteRepository.deleteAll();
        produtoRepository.deleteAll();
        usuarioRepository.deleteAll();

        Cliente c = clienteRepository.save(Cliente.builder()
            .razaoSocial("Almeida & Filhos Ltda.").documento("12.345.678/0001-90").ativo("S").build());
        clienteId = c.getId();

        // Cria usuário real para satisfazer FK fk_pedido_usuario
        Usuario u = usuarioRepository.save(Usuario.builder()
            .login("operador.test").nome("Operador Teste")
            .senhaHash("$2a$10$hash_placeholder")
            .perfil(PerfilUsuario.OPERADOR).ativo("S").build());
        usuarioId = u.getId();

        Produto p1 = produtoRepository.save(Produto.builder()
            .codigo("NOTE-PRO-14").descricao("Notebook Pro").unidadeMedida("UN")
            .precoVenda(new BigDecimal("4899.90")).estoqueAtual(new BigDecimal("18.000")).ativo("S").build());
        produtoId1 = p1.getId();

        Produto p2 = produtoRepository.save(Produto.builder()
            .codigo("MON-27-QHD").descricao("Monitor QHD").unidadeMedida("UN")
            .precoVenda(new BigDecimal("1599.00")).estoqueAtual(new BigDecimal("10.000")).ativo("S").build());
        produtoId2 = p2.getId();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private PedidoRequestDTO dtoPedido() {
        return new PedidoRequestDTO(clienteId, usuarioId, "Observação de teste",
            List.of(new ItemPedidoRequestDTO(produtoId1, new BigDecimal("2.000"), null)));
    }

    /** Cria um pedido e retorna o header Location. */
    private String criarPedido() throws Exception {
        return mvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoPedido())))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");
    }

    // ── POST /pedidos ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /pedidos: deve criar pedido com status ABERTO e total calculado")
    void post_criarPedido_201() throws Exception {
        mvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoPedido())))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.status").value("ABERTO"))
            // 2 × 4899.90 = 9799.80
            .andExpect(jsonPath("$.valorTotal").value(9799.80))
            .andExpect(jsonPath("$.itens", hasSize(1)))
            .andExpect(jsonPath("$.itens[0].precoUnitario").value(4899.90))
            .andExpect(jsonPath("$.razaoSocialCliente").isString());
    }

    @Test
    @DisplayName("POST /pedidos: deve descontar estoque do produto ao criar pedido")
    void post_criarPedido_descontaEstoque() throws Exception {
        criarPedido();

        mvc.perform(get("/produtos/" + produtoId1))
            .andExpect(status().isOk())
            // 18 - 2 = 16
            .andExpect(jsonPath("$.estoqueAtual").value(16.0));
    }

    @Test
    @DisplayName("POST /pedidos: deve retornar 422 quando lista de itens está vazia")
    void post_semItens_422() throws Exception {
        // Bean Validation falha antes de chegar no service — usuarioId não precisa existir
        PedidoRequestDTO dto = new PedidoRequestDTO(clienteId, usuarioId, null, List.of());

        mvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.campos.itens").exists());
    }

    @Test
    @DisplayName("POST /pedidos: deve retornar 404 para cliente inexistente")
    void post_clienteInexistente_404() throws Exception {
        // Usa usuarioId real para não disparar UsuarioNaoEncontradoException (404) antes
        PedidoRequestDTO dto = new PedidoRequestDTO(9999L, usuarioId, null,
            List.of(new ItemPedidoRequestDTO(produtoId1, BigDecimal.ONE, null)));

        mvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /pedidos: deve retornar 422 para produto inativo — 'Produto inexistente ou inativo.'")
    void post_produtoInativo_422() throws Exception {
        Produto inativo = produtoRepository.save(Produto.builder()
            .codigo("INATIVO").descricao("Inativo").unidadeMedida("UN")
            .precoVenda(BigDecimal.ONE).estoqueAtual(BigDecimal.ZERO).ativo("N").build());

        // Usa usuarioId real — service valida usuário antes do produto
        PedidoRequestDTO dto = new PedidoRequestDTO(clienteId, usuarioId, null,
            List.of(new ItemPedidoRequestDTO(inativo.getId(), BigDecimal.ONE, null)));

        mvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.title").value("Produto inexistente ou inativo"));
    }

    // ── GET /pedidos e GET /pedidos/{id} ──────────────────────────────────────

    @Test
    @DisplayName("GET /pedidos/{id}: deve retornar pedido com itens completos")
    void get_porId_200() throws Exception {
        String loc = criarPedido();

        mvc.perform(get(loc))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itens", hasSize(1)))
            .andExpect(jsonPath("$.itens[0].codigoProduto").value("NOTE-PRO-14"));
    }

    @Test
    @DisplayName("GET /pedidos/{id}: deve retornar 404 para ID inexistente")
    void get_porId_naoEncontrado_404() throws Exception {
        mvc.perform(get("/pedidos/9999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Pedido não encontrado"));
    }

    @Test
    @DisplayName("GET /pedidos?status=ABERTO: deve filtrar por status")
    void get_porStatus_200() throws Exception {
        criarPedido();

        mvc.perform(get("/pedidos").param("status", "ABERTO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].status").value("ABERTO"));

        mvc.perform(get("/pedidos").param("status", "CANCELADO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    // ── Máquina de estados ────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /confirmar: ABERTO → CONFIRMADO")
    void patch_confirmar_200() throws Exception {
        String loc = criarPedido();

        mvc.perform(patch(loc + "/confirmar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMADO"));
    }

    @Test
    @DisplayName("PATCH /faturar: CONFIRMADO → FATURADO")
    void patch_faturar_200() throws Exception {
        String loc = criarPedido();
        mvc.perform(patch(loc + "/confirmar"));
        mvc.perform(patch(loc + "/faturar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FATURADO"));
    }

    @Test
    @DisplayName("PATCH /faturar: deve retornar 409 se pedido ABERTO — máquina de estados")
    void patch_faturar_aberto_409() throws Exception {
        String loc = criarPedido();

        mvc.perform(patch(loc + "/faturar"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Transição de status inválida"));
    }

    @Test
    @DisplayName("PATCH /cancelar: deve cancelar pedido ABERTO e restituir estoque")
    void patch_cancelar_200() throws Exception {
        String loc = criarPedido();

        mvc.perform(patch(loc + "/cancelar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELADO"));

        // estoque restituído: 18 - 2 + 2 = 18
        mvc.perform(get("/produtos/" + produtoId1))
            .andExpect(jsonPath("$.estoqueAtual").value(18.0));
    }

    @Test
    @DisplayName("PATCH /cancelar: deve retornar 409 para pedido FATURADO — legado não validava")
    void patch_cancelar_faturado_409() throws Exception {
        String loc = criarPedido();
        mvc.perform(patch(loc + "/confirmar"));
        mvc.perform(patch(loc + "/faturar"));

        mvc.perform(patch(loc + "/cancelar"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Transição de status inválida"));
    }

    // ── POST /pedidos/{id}/itens ──────────────────────────────────────────────

    @Test
    @DisplayName("POST /itens: deve adicionar segundo item e recalcular total")
    void post_adicionarItem_200() throws Exception {
        String loc = criarPedido();

        mvc.perform(post(loc + "/itens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                    new ItemPedidoRequestDTO(produtoId2, new BigDecimal("1.000"), null))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itens", hasSize(2)))
            // 9799.80 + 1 × 1599.00 = 11398.80
            .andExpect(jsonPath("$.valorTotal").value(11398.80));
    }

    @Test
    @DisplayName("POST /itens: deve retornar 409 para produto duplicado — uq_item_pedido")
    void post_adicionarItemDuplicado_409() throws Exception {
        String loc = criarPedido();

        mvc.perform(post(loc + "/itens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                    new ItemPedidoRequestDTO(produtoId1, BigDecimal.ONE, null))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Item duplicado no pedido"));
    }

    @Test
    @DisplayName("POST /itens: deve retornar 409 para pedido não ABERTO")
    void post_adicionarItem_pedidoFechado_409() throws Exception {
        String loc = criarPedido();
        mvc.perform(patch(loc + "/confirmar"));

        mvc.perform(post(loc + "/itens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                    new ItemPedidoRequestDTO(produtoId2, BigDecimal.ONE, null))))
            .andExpect(status().isConflict());
    }

    // ── DELETE /pedidos/{id}/itens/{produtoId} ────────────────────────────────

    @Test
    @DisplayName("DELETE /itens/{produtoId}: deve remover item e recalcular total")
    void delete_removerItem_200() throws Exception {
        // cria pedido com 2 itens
        PedidoRequestDTO dto = new PedidoRequestDTO(clienteId, usuarioId, null, List.of(
            new ItemPedidoRequestDTO(produtoId1, new BigDecimal("2.000"), null),
            new ItemPedidoRequestDTO(produtoId2, new BigDecimal("1.000"), null)
        ));
        String loc = mvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andReturn().getResponse().getHeader("Location");

        mvc.perform(delete(loc + "/itens/" + produtoId1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itens", hasSize(1)))
            // apenas produto2: 1 × 1599.00
            .andExpect(jsonPath("$.valorTotal").value(1599.0));
    }

    // ── DELETE /pedidos/{id} ──────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /pedidos/{id}: deve excluir pedido e itens — pedido_excluir()")
    void delete_excluirPedido_204() throws Exception {
        String loc = criarPedido();

        mvc.perform(delete(loc))
            .andExpect(status().isNoContent());

        mvc.perform(get(loc))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /pedidos/{id}: deve retornar 404 para ID inexistente")
    void delete_naoEncontrado_404() throws Exception {
        mvc.perform(delete("/pedidos/9999"))
            .andExpect(status().isNotFound());
    }
}
