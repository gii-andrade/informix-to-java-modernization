package br.com.pedidos.usuario.controller;

import br.com.pedidos.usuario.dto.AlterarSenhaDTO;
import br.com.pedidos.usuario.dto.UsuarioRequestDTO;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração do UsuarioController com H2 em memória.
 *
 * Exercita o fluxo completo: Controller → Service (BCrypt real) → Repository → H2.
 * Cobre todos os endpoints, incluindo os que suprem lacunas do legado:
 *   alterar / alterarSenha / reativar / excluir / listarPorPerfil.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class UsuarioControllerIT {

    @Autowired MockMvc           mvc;
    @Autowired ObjectMapper      mapper;
    @Autowired UsuarioRepository repository;

    @BeforeEach
    void limparBase() {
        repository.deleteAll();
    }

    private UsuarioRequestDTO dtoValido() {
        return new UsuarioRequestDTO(
            "carlos.silva", "Carlos Eduardo Silva", "senha@123", PerfilUsuario.OPERADOR
        );
    }

    /** Cria usuário e retorna header Location. */
    private String criarUsuario() throws Exception {
        return mvc.perform(post("/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoValido())))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");
    }

    // ── POST /usuarios ────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /usuarios: deve criar usuário com perfil OPERADOR e retornar 201")
    void post_criarUsuario_201() throws Exception {
        mvc.perform(post("/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoValido())))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.login").value("carlos.silva"))
            .andExpect(jsonPath("$.perfil").value("OPERADOR"))
            .andExpect(jsonPath("$.ativo").value("S"))
            // senha NUNCA exposta na resposta
            .andExpect(jsonPath("$.senhaHash").doesNotExist())
            .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    @DisplayName("POST /usuarios: deve retornar 409 com login duplicado — uq_usuario_login")
    void post_loginDuplicado_409() throws Exception {
        criarUsuario();

        mvc.perform(post("/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dtoValido())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Login já cadastrado"));
    }

    @Test
    @DisplayName("POST /usuarios: deve retornar 422 quando campos obrigatórios estão em branco")
    void post_camposObrigatorios_422() throws Exception {
        UsuarioRequestDTO dto = new UsuarioRequestDTO("", "", "", null);

        mvc.perform(post("/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.campos.login").exists())
            .andExpect(jsonPath("$.campos.nome").exists())
            .andExpect(jsonPath("$.campos.senha").exists());
    }

    @Test
    @DisplayName("POST /usuarios: deve retornar 422 para senha com menos de 8 caracteres")
    void post_senhaCurta_422() throws Exception {
        UsuarioRequestDTO dto = new UsuarioRequestDTO(
            "user.test", "User Test", "curta", null
        );

        mvc.perform(post("/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.campos.senha").exists());
    }

    // ── GET /usuarios e GET /usuarios/{id} ───────────────────────────────────

    @Test
    @DisplayName("GET /usuarios: deve listar todos os usuários sem expor senhas")
    void get_listarTodos_200() throws Exception {
        criarUsuario();

        mvc.perform(get("/usuarios"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].login").value("carlos.silva"))
            .andExpect(jsonPath("$[0].senhaHash").doesNotExist());
    }

    @Test
    @DisplayName("GET /usuarios?perfil=OPERADOR: deve filtrar por perfil")
    void get_porPerfil_200() throws Exception {
        criarUsuario();
        mvc.perform(post("/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                    new UsuarioRequestDTO("admin.user", "Admin User", "senha@123", PerfilUsuario.ADMIN)
                )));

        mvc.perform(get("/usuarios").param("perfil", "OPERADOR"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].perfil").value("OPERADOR"));
    }

    @Test
    @DisplayName("GET /usuarios/{id}: deve retornar 404 para ID inexistente")
    void get_naoEncontrado_404() throws Exception {
        mvc.perform(get("/usuarios/9999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Usuário não encontrado"));
    }

    // ── PUT /usuarios/{id} ────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /usuarios/{id}: deve atualizar login, nome e perfil — lacuna corrigida")
    void put_alterar_200() throws Exception {
        String loc = criarUsuario();

        UsuarioRequestDTO alterado = new UsuarioRequestDTO(
            "carlos.atualizado", "Carlos Atualizado", "qualquer@123", PerfilUsuario.ADMIN
        );

        mvc.perform(put(loc)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(alterado)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value("carlos.atualizado"))
            .andExpect(jsonPath("$.perfil").value("ADMIN"));
    }

    // ── PATCH /usuarios/{id}/senha ────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /senha: deve alterar senha e retornar 204 — lacuna de segurança corrigida")
    void patch_alterarSenha_204() throws Exception {
        String loc = criarUsuario();

        AlterarSenhaDTO dto = new AlterarSenhaDTO("senha@123", "novaSenha@456");

        mvc.perform(patch(loc + "/senha")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /senha: deve retornar 422 para senha atual incorreta")
    void patch_senhaAtualErrada_422() throws Exception {
        String loc = criarUsuario();

        AlterarSenhaDTO dto = new AlterarSenhaDTO("senhaErrada", "novaSenha@456");

        mvc.perform(patch(loc + "/senha")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.title").value("Senha atual incorreta"));
    }

    // ── PATCH /usuarios/{id}/desativar ────────────────────────────────────────

    @Test
    @DisplayName("PATCH /desativar: deve retornar ativo='N' — usuario_desativar()")
    void patch_desativar_200() throws Exception {
        String loc = criarUsuario();

        mvc.perform(patch(loc + "/desativar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ativo").value("N"));
    }

    // ── PATCH /usuarios/{id}/reativar ─────────────────────────────────────────

    @Test
    @DisplayName("PATCH /reativar: deve retornar ativo='S' — lacuna corrigida (desativação era irreversível)")
    void patch_reativar_200() throws Exception {
        String loc = criarUsuario();
        mvc.perform(patch(loc + "/desativar"));

        mvc.perform(patch(loc + "/reativar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ativo").value("S"));
    }

    // ── DELETE /usuarios/{id} ─────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /usuarios/{id}: deve excluir e retornar 204")
    void delete_excluir_204() throws Exception {
        String loc = criarUsuario();

        mvc.perform(delete(loc))
            .andExpect(status().isNoContent());

        mvc.perform(get(loc))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /usuarios/{id}: deve retornar 404 para ID inexistente")
    void delete_naoEncontrado_404() throws Exception {
        mvc.perform(delete("/usuarios/9999"))
            .andExpect(status().isNotFound());
    }
}
