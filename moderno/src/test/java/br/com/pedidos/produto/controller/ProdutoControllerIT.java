package br.com.pedidos.produto.controller;

import br.com.pedidos.produto.dto.ProdutoRequestDTO;
import br.com.pedidos.produto.repository.ProdutoRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração do ProdutoController usando H2 em memória.
 *
 * Exercita o fluxo completo: Controller → Service → Repository → H2.
 * Cobre todos os endpoints — incluindo os dois que suprem lacunas do legado:
 *   PUT  /produtos/{id}         → produto_alterar() que não existia
 *   PATCH /produtos/{id}/preco  → ajuste de preço sem correspondente no legado
 *   PATCH /produtos/{id}/estoque → ajuste de estoque sem correspondente no legado
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class ProdutoControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ProdutoRepository repository;

    @BeforeEach
    void limparBase() {
        repository.deleteAll();
    }

    private ProdutoRequestDTO dtoValido() {
        return new ProdutoRequestDTO(
            "NOTE-PRO-14",
            "Notebook Pro 14 polegadas, 16 GB RAM, 512 GB SSD",
            "UN",
            new BigDecimal("4899.90"),
            new BigDecimal("18.000"),
            "S"
        );
    }

    // -------------------------------------------------------------------------
    // POST /produtos
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /produtos: deve criar produto e retornar 201 com Location")
    void post_criarProduto_201() throws Exception {
        mvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoValido())))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.codigo").value("NOTE-PRO-14"))
            .andExpect(jsonPath("$.unidadeMedida").value("UN"))
            .andExpect(jsonPath("$.precoVenda").value(4899.90))
            .andExpect(jsonPath("$.ativo").value("S"));
    }

    @Test
    @DisplayName("POST /produtos: deve retornar 409 com código duplicado — uq_produto_codigo")
    void post_codigoDuplicado_409() throws Exception {
        mvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoValido())))
            .andExpect(status().isCreated());

        mvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoValido())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Código de produto já cadastrado"));
    }

    @Test
    @DisplayName("POST /produtos: deve retornar 422 quando campos obrigatórios estão em branco")
    void post_camposObrigatorios_422() throws Exception {
        ProdutoRequestDTO dto = new ProdutoRequestDTO(
            "", null, null, null, null, "S"
        );

        mvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.campos.codigo").exists())
            .andExpect(jsonPath("$.campos.descricao").exists())
            .andExpect(jsonPath("$.campos.precoVenda").exists());
    }

    @Test
    @DisplayName("POST /produtos: deve retornar 422 quando preço é negativo — ck_produto_preco")
    void post_precoNegativo_422() throws Exception {
        ProdutoRequestDTO dto = new ProdutoRequestDTO(
            "PROD-X", "Produto X", "UN",
            new BigDecimal("-1.00"), BigDecimal.ZERO, "S"
        );

        mvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.campos.precoVenda").exists());
    }

    @Test
    @DisplayName("POST /produtos: deve retornar 422 quando estoque é negativo — ck_produto_estoque")
    void post_estoqueNegativo_422() throws Exception {
        ProdutoRequestDTO dto = new ProdutoRequestDTO(
            "PROD-Y", "Produto Y", "UN",
            new BigDecimal("10.00"), new BigDecimal("-1.000"), "S"
        );

        mvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.campos.estoqueAtual").exists());
    }

    // -------------------------------------------------------------------------
    // GET /produtos e GET /produtos/{id}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /produtos: deve listar todos os produtos")
    void get_listarTodos_200() throws Exception {
        mvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoValido())))
            .andExpect(status().isCreated());

        mvc.perform(get("/produtos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].codigo").value("NOTE-PRO-14"));
    }

    @Test
    @DisplayName("GET /produtos/{id}: deve retornar 200 com dados do produto")
    void get_porId_200() throws Exception {
        String location = mvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoValido())))
            .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codigo").value("NOTE-PRO-14"))
            .andExpect(jsonPath("$.estoqueAtual").value(18.0));
    }

    @Test
    @DisplayName("GET /produtos/{id}: deve retornar 404 para ID inexistente")
    void get_porId_naoEncontrado_404() throws Exception {
        mvc.perform(get("/produtos/9999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Produto não encontrado"));
    }

    // -------------------------------------------------------------------------
    // PUT /produtos/{id} — lacuna corrigida (produto_alterar não existia)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PUT /produtos/{id}: deve atualizar todos os campos e retornar 200")
    void put_alterar_200() throws Exception {
        String location = mvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoValido())))
            .andReturn().getResponse().getHeader("Location");

        ProdutoRequestDTO alterado = new ProdutoRequestDTO(
            "NOTE-PRO-14", "Notebook Pro ATUALIZADO", "UN",
            new BigDecimal("4599.90"), new BigDecimal("20.000"), "N"
        );

        mvc.perform(put(location)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(alterado)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.descricao").value("Notebook Pro ATUALIZADO"))
            .andExpect(jsonPath("$.precoVenda").value(4599.90))
            .andExpect(jsonPath("$.ativo").value("N"));
    }

    // -------------------------------------------------------------------------
    // PATCH /produtos/{id}/preco
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PATCH /produtos/{id}/preco: deve atualizar apenas o preço")
    void patch_alterarPreco_200() throws Exception {
        String location = mvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoValido())))
            .andReturn().getResponse().getHeader("Location");

        mvc.perform(patch(location + "/preco")
                .param("valor", "3999.00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.precoVenda").value(3999.00))
            // demais campos não mudam
            .andExpect(jsonPath("$.codigo").value("NOTE-PRO-14"))
            .andExpect(jsonPath("$.estoqueAtual").value(18.0));
    }

    @Test
    @DisplayName("PATCH /produtos/{id}/preco: deve retornar 400 para preço negativo")
    void patch_alterarPreco_negativo_400() throws Exception {
        String location = mvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoValido())))
            .andReturn().getResponse().getHeader("Location");

        mvc.perform(patch(location + "/preco")
                .param("valor", "-50.00"))
            .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // PATCH /produtos/{id}/estoque
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PATCH /produtos/{id}/estoque: deve atualizar apenas o estoque")
    void patch_ajustarEstoque_200() throws Exception {
        String location = mvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoValido())))
            .andReturn().getResponse().getHeader("Location");

        mvc.perform(patch(location + "/estoque")
                .param("valor", "100.500"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estoqueAtual").value(100.5))
            // preço não muda
            .andExpect(jsonPath("$.precoVenda").value(4899.90));
    }

    // -------------------------------------------------------------------------
    // DELETE /produtos/{id}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /produtos/{id}: deve excluir produto sem itens e retornar 204")
    void delete_excluir_204() throws Exception {
        String location = mvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoValido())))
            .andReturn().getResponse().getHeader("Location");

        mvc.perform(delete(location))
            .andExpect(status().isNoContent());

        mvc.perform(get(location))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /produtos/{id}: deve retornar 404 para produto inexistente")
    void delete_naoEncontrado_404() throws Exception {
        mvc.perform(delete("/produtos/9999"))
            .andExpect(status().isNotFound());
    }
}
