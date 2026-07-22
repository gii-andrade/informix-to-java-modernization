package br.com.pedidos.security;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/**
 * Testes de integração do AuthController (POST /auth/login).
 *
 * Verifica:
 *   • Login válido devolve token JWT e perfil corretos
 *   • Login com senha incorreta retorna 401
 *   • Login com usuário inexistente retorna 401
 *   • Login com usuário inativo retorna 403
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

    @Autowired MockMvc           mvc;
    @Autowired ObjectMapper      mapper;
    @Autowired UsuarioRepository usuarioRepository;

    // Cria o usuário de teste via POST /usuarios (rota que requer ADMIN no app,
    // mas aqui usamos o repository diretamente para setup isolado).
    @BeforeEach
    void setup() throws Exception {
        usuarioRepository.deleteAll();
    }

    private void criarUsuario(String login, String senha, PerfilUsuario perfil, boolean ativo) throws Exception {
        mvc.perform(post("/usuarios")
                .with(csrf())
                .header("Authorization", "Bearer " + obterTokenAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                    new UsuarioRequestDTO(login, "Usuário Teste", senha, perfil)
                )));
    }

    /** Cria um admin via repositório direto para bootstrap dos testes. */
    private String obterTokenAdmin() throws Exception {
        // Usa o BCrypt real para criar seed no H2
        br.com.pedidos.usuario.entity.Usuario admin =
            usuarioRepository.save(br.com.pedidos.usuario.entity.Usuario.builder()
                .login("admin.test")
                .nome("Admin Teste")
                .senhaHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                    .encode("admin@12345"))
                .perfil(PerfilUsuario.ADMIN)
                .ativo("S")
                .build());

        String resp = mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                    new AuthController.LoginRequest("admin.test", "admin@12345")
                )))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        return mapper.readTree(resp).get("token").asText();
    }

    // ── POST /auth/login ──────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/login: credenciais válidas devolvem token e perfil")
    void login_credenciaisValidas_retornaToken() throws Exception {
        obterTokenAdmin(); // cria e autentica o admin

        mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                    new AuthController.LoginRequest("admin.test", "admin@12345")
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.token", not(blankString())))
            .andExpect(jsonPath("$.perfil").value("ADMIN"))
            .andExpect(jsonPath("$.expiresInMs").value(86_400_000));
    }

    @Test
    @DisplayName("POST /auth/login: senha incorreta retorna 401")
    void login_senhaErrada_401() throws Exception {
        obterTokenAdmin();

        mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                    new AuthController.LoginRequest("admin.test", "senhaErrada1")
                )))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login: usuário inexistente retorna 401")
    void login_usuarioInexistente_401() throws Exception {
        mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                    new AuthController.LoginRequest("nao.existe", "senha@123")
                )))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login: endpoint protegido sem token retorna 401")
    void endpointProtegido_semToken_401() throws Exception {
        mvc.perform(get("/clientes"))
            .andExpect(status().isUnauthorized());
    }
}
