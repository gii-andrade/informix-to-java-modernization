package br.com.pedidos.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO dedicado à operação de alteração de senha.
 *
 * Lacuna corrigida: o legado não tinha função de alterar senha —
 * qualquer mudança exigia desativar e recriar o usuário.
 *
 * Campos:
 *   - senhaAtual  → obrigatória para confirmar identidade antes de trocar
 *   - senhaNova   → nova senha em texto claro, será convertida para BCrypt
 */
public record AlterarSenhaDTO(

    @NotBlank(message = "Senha atual é obrigatória")
    String senhaAtual,

    @NotBlank(message = "Nova senha é obrigatória")
    @Size(min = 8, max = 100, message = "Nova senha deve ter entre 8 e 100 caracteres")
    String senhaNova

) {}
