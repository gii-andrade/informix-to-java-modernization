package br.com.pedidos.usuario.dto;

import br.com.pedidos.usuario.entity.Usuario;
import br.com.pedidos.usuario.enums.PerfilUsuario;

import java.time.LocalDate;

/**
 * DTO de saída para usuário.
 *
 * NUNCA expõe senhaHash — campo omitido intencionalmente.
 * No legado, o campo senha era INVISIBLE no formulário mas ainda trafegava
 * internamente. A API garante que o hash jamais saia nas respostas.
 *
 * Expõe todos os demais campos, incluindo dataCadastro que o formulário
 * legado usuario.per não exibia na TUI.
 */
public record UsuarioResponseDTO(
    Long id,
    String login,
    String nome,
    PerfilUsuario perfil,
    String ativo,
    LocalDate dataCadastro
) {

    /** Converte uma entidade {@link Usuario} para o DTO de resposta. */
    public static UsuarioResponseDTO from(Usuario usuario) {
        return new UsuarioResponseDTO(
            usuario.getId(),
            usuario.getLogin(),
            usuario.getNome(),
            usuario.getPerfil(),
            usuario.getAtivo(),
            usuario.getDataCadastro()
        );
    }
}
