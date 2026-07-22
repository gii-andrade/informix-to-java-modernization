package br.com.pedidos.relatorio.controller;

import br.com.pedidos.cliente.entity.Cliente;
import br.com.pedidos.cliente.repository.ClienteRepository;
import br.com.pedidos.pedido.dto.ItemPedidoRequestDTO;
import br.com.pedidos.pedido.dto.PedidoRequestDTO;
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
 * Testes de integração do RelatorioController com H2 em memória.
 *
 * Exercita o fluxo completo: Controller → Service → Repository → H2.
 *
 * Porta relatorio_pedidos_status() do relatorio.4gl:
 *   • Retorna agrupamento por status com quantidade e valor total
 *   • Retorna lista vazia quando não há pedidos
 *   • Retorna apenas os status presentes na base (não exibe zeros para ausentes)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class RelatorioControllerIT {

    @Autowired MockMvc           mvc;
    @Autowired ObjectMapper      mapper;
    @Autowired PedidoRepository  pedidoRepository;
    @Autowired ClienteRepository clienteRepository;
    @Autowired ProdutoRepository produtoRepository;
    @Autowired UsuarioRepository usuarioRepository;

    private Long clienteId;
    private Long usuarioId;
    private Long produtoId;

    @BeforeEach
    void setup() {
        pedidoRepository.deleteAll();
        clienteRepository.deleteAll();
        produtoRepository.deleteAll();
        usuarioRepository.deleteAll();

        Cliente c = clienteRepository.save(Cliente.builder()
            .razaoSocial("Almeida & Filhos Ltda.").documento("12.345.678/0001-90").ativo("S").build());
        clienteId = c.getId();

        Usuario u = usuarioRepository.save(Usuario.builder()
            .login("operador.relatorio").nome("Operador Relatório")
            .senhaHash("$2a$10$hash_placeholder")
            .perfil(PerfilUsuario.OPERADOR).ativo("S").build());
        usuarioId = u.getId();

        Produto p = produtoRepository.save(Produto.builder()
            .codigo("PROD-REL-01").descricao("Produto Relatório").unidadeMedida("UN")
            .precoVenda(new BigDecimal("100.00")).estoqueAtual(new BigDecimal("50.000")).ativo("S").build());
        produtoId = p.getId();
    }

    // ── Auxiliar ─────────────────────────────────────────────────────────────

    /** Cria um pedido via POST /pedidos e retorna o URI de Location. */
    private String criarPedido() throws Exception {
        PedidoRequestDTO dto = new PedidoRequestDTO(
            clienteId, usuarioId, null,
            List.of(new ItemPedidoRequestDTO(produtoId, new BigDecimal("2.000"), null))
        );
        return mvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");
    }

    /** Extrai o ID numérico do URI de Location. */
    private String idDe(String location) {
        return location.substring(location.lastIndexOf('/') + 1);
    }

    // ── GET /relatorios/pedidos-por-status ────────────────────────────────────

    @Test
    @DisplayName("GET /relatorios/pedidos-por-status: retorna lista vazia quando não há pedidos")
    void get_semPedidos_retornaListaVazia() throws Exception {
        mvc.perform(get("/relatorios/pedidos-por-status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /relatorios/pedidos-por-status: retorna ABERTO com 1 pedido após criação")
    void get_umPedidoAberto_retornaAbertoComQuantidade1() throws Exception {
        criarPedido();

        mvc.perform(get("/relatorios/pedidos-por-status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].status").value("ABERTO"))
            .andExpect(jsonPath("$[0].quantidade").value(1))
            // 2 × 100.00 = 200.00
            .andExpect(jsonPath("$[0].valorTotal").value(200.00));
    }

    @Test
    @DisplayName("GET /relatorios/pedidos-por-status: agrega corretamente dois pedidos no mesmo status")
    void get_doisPedidosAbertos_somaValores() throws Exception {
        criarPedido();
        criarPedido();

        mvc.perform(get("/relatorios/pedidos-por-status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].status").value("ABERTO"))
            .andExpect(jsonPath("$[0].quantidade").value(2))
            // 2 × (2 × 100.00) = 400.00
            .andExpect(jsonPath("$[0].valorTotal").value(400.00));
    }

    @Test
    @DisplayName("GET /relatorios/pedidos-por-status: exibe ABERTO e CANCELADO separadamente após cancelamento")
    void get_pedidoAbertoECancelado_exibeDoisStatus() throws Exception {
        String location1 = criarPedido();
        criarPedido();

        // Cancela o primeiro pedido
        mvc.perform(patch("/pedidos/" + idDe(location1) + "/cancelar"))
            .andExpect(status().isOk());

        mvc.perform(get("/relatorios/pedidos-por-status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            // ORDER BY status — ABERTO < CANCELADO
            .andExpect(jsonPath("$[0].status").value("ABERTO"))
            .andExpect(jsonPath("$[0].quantidade").value(1))
            .andExpect(jsonPath("$[1].status").value("CANCELADO"))
            .andExpect(jsonPath("$[1].quantidade").value(1));
    }

    @Test
    @DisplayName("GET /relatorios/pedidos-por-status: exibe ABERTO, CONFIRMADO e FATURADO após ciclo completo")
    void get_cicloCompleto_exibeTresStatus() throws Exception {
        String locAberto     = criarPedido();
        String locConfirmado = criarPedido();
        String locFaturado   = criarPedido();

        mvc.perform(patch("/pedidos/" + idDe(locConfirmado) + "/confirmar"))
            .andExpect(status().isOk());

        mvc.perform(patch("/pedidos/" + idDe(locFaturado) + "/confirmar"))
            .andExpect(status().isOk());
        mvc.perform(patch("/pedidos/" + idDe(locFaturado) + "/faturar"))
            .andExpect(status().isOk());

        mvc.perform(get("/relatorios/pedidos-por-status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[?(@.status=='ABERTO')].quantidade",    contains(1)))
            .andExpect(jsonPath("$[?(@.status=='CONFIRMADO')].quantidade", contains(1)))
            .andExpect(jsonPath("$[?(@.status=='FATURADO')].quantidade",   contains(1)));
    }
}
