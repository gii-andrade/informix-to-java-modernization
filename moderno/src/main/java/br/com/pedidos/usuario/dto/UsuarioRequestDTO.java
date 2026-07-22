package br.com.pedidos.usuario.dto;

import br.com.pedidos.usuario.enums.PerfilUsuario;
import jakarta.validation.constraints.*;
import lombok.Builder;

/**
 * DTO de entrada para criação e atualização de usuários.
 *
 * Validações mapeadas a partir do legado:
 *   - login   → campo REQUIRED no usuario.per + UNIQUE (uq_usuario_login)
 *   - nome    → campo REQUIRED no usuario.per + NOT NULL no schema
 *   - senha   → campo REQUIRED + INVISIBLE no usuario.per
 *               Mínimo de 8 caracteres — requisito de segurança ausente no legado
 *   - perfil  → campo REQUIRED + ck_usuario_perfil: ADMIN | OPERADOR | CONSULTA
 *               DEFAULT 'OPERADOR' quando não informado (LET perfil = 'OPERADOR', linha 16)
 *
 * senha NÃO é exposta nas respostas — apenas no request de criação/alteração.
 * O service aplica BCrypt antes de persistir (lacuna de segurança corrigida).
 */
@Builder
public record UsuarioRequestDTO(

    @NotBlank(message = "Login é obrigatório")
    @Size(max = 50, message = "Login deve ter no máximo 50 caracteres")
    @Pattern(
        regexp = "^[a-zA-Z0-9._-]+$",
        message = "Login deve conter apenas letras, números, ponto, hífen ou sublinhado"
    )
    String login,

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
    String nome,

    /**
     * Senha em texto claro — será convertida para BCrypt pelo service.
     * Nunca armazenada ou retornada sem hash.
     * Mínimo de 8 caracteres imposto para corrigir ausência de política de senha no legado.
     */
    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres")
    String senha,

    /**
     * Perfil de acesso — ck_usuario_perfil: ADMIN | OPERADOR | CONSULTA.
     * Quando nulo, o service aplica OPERADOR como padrão
     * (LET perfil = 'OPERADOR', usuario_incluir linha 16).
     */
    PerfilUsuario perfil

) {}
