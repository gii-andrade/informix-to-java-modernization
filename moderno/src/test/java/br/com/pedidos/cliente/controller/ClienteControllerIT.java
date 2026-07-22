package br.com.pedidos.cliente.controller;

import br.com.pedidos.cliente.dto.ClienteRequestDTO;
import br.com.pedidos.cliente.repository.ClienteRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração do ClienteController usando H2 em memória.
 *
 * Exercita o fluxo completo: Controller → Service → Repository → H2.
 * Cobre os endpoints mapeados a partir das funções do legado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class ClienteControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ClienteRepository repository;

    @BeforeEach
    void limparBase() {
        repository.deleteAll();
    }

    private ClienteRequestDTO dtoValido() {
        return new ClienteRequestDTO(
            "Almeida & Filhos Ltda.", "Almeida Materiais",
            "12.345.678/0001-90", "compras@almeida.com.br",
            "(11) 3456-1200", "Rua das Acácias, 245",
            "São Paulo", "SP", "04567-120", "S"
        );
    }

    // -------------------------------------------------------------------------
    // POST /clientes
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /clientes: deve criar cliente e retornar 201 com Location")
    void post_criarCliente_201() throws Exception {
        mvc.perform(post("/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoValido())))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.razaoSocial").value("Almeida & Filhos Ltda."))
            .andExpect(jsonPath("$.ativo").value("S"));
    }

    @Test
    @DisplayName("POST /clientes: deve retornar 409 com documento duplicado")
    void post_documentoDuplicado_409() throws Exception {
        mvc.perform(post("/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoValido())))
            .andExpect(status().isCreated());

        mvc.perform(post("/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoValido())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Documento já cadastrado"));
    }

    @Test
    @DisplayName("POST /clientes: deve retornar 422 quando razaoSocial está em branco")
    void post_razaoSocialVazia_422() throws Exception {
        ClienteRequestDTO dto = new ClienteRequestDTO(
            "", null, "00.000.000/0001-00",
            null, null, null, null, null, null, "S"
        );

        mvc.perform(post("/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.campos.razaoSocial").exists());
    }

    // -------------------------------------------------------------------------
    // GET /clientes/{id}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /clientes/{id}: deve retornar 200 com dados do cliente")
    void get_porId_200() throws Exception {
        String location = mvc.perform(post("/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoValido())))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

        mvc.perform(get(location))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.documento").value("12.345.678/0001-90"));
    }

    @Test
    @DisplayName("GET /clientes/{id}: deve retornar 404 para ID inexistente")
    void get_porId_naoEncontrado_404() throws Exception {
        mvc.perform(get("/clientes/9999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Cliente não encontrado"));
    }

    // -------------------------------------------------------------------------
    // PUT /clientes/{id}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PUT /clientes/{id}: deve atualizar e retornar 200")
    void put_alterar_200() throws Exception {
        String location = mvc.perform(post("/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoValido())))
            .andReturn().getResponse().getHeader("Location");

        ClienteRequestDTO alterado = new ClienteRequestDTO(
            "Almeida Atualizado", null,
            "12.345.678/0001-90", "novo@email.com",
            null, null, null, null, null, "N"
        );

        mvc.perform(put(location)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(alterado)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.razaoSocial").value("Almeida Atualizado"))
            .andExpect(jsonPath("$.ativo").value("N"));
    }

    // -------------------------------------------------------------------------
    // DELETE /clientes/{id}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /clientes/{id}: deve excluir e retornar 204")
    void delete_excluir_204() throws Exception {
        String location = mvc.perform(post("/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoValido())))
            .andReturn().getResponse().getHeader("Location");

        mvc.perform(delete(location))
            .andExpect(status().isNoContent());

        mvc.perform(get(location))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /clientes/{id}: deve retornar 404 para cliente inexistente")
    void delete_naoEncontrado_404() throws Exception {
        mvc.perform(delete("/clientes/9999"))
            .andExpect(status().isNotFound());
    }
}
