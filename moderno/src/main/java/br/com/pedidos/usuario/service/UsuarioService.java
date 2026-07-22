package br.com.pedidos.usuario.service;

import br.com.pedidos.usuario.dto.AlterarSenhaDTO;
import br.com.pedidos.usuario.dto.UsuarioRequestDTO;
import br.com.pedidos.usuario.dto.UsuarioResponseDTO;
import br.com.pedidos.usuario.enums.PerfilUsuario;

import java.util.List;

/**
 * Contrato do serviço de usuários.
 *
 * Operações mapeadas a partir do legado usuario.4gl:
 *   incluir     → usuario_incluir()
 *   desativar   → usuario_desativar()
 *
 * Operações novas — lacunas do legado corrigidas:
 *   buscarPorId   → sem correspondente (TUI não consultava por ID)
 *   listarTodos   → sem correspondente
 *   listarPorPerfil → sem correspondente
 *   alterar       → sem correspondente (não havia edição de dados)
 *   reativar      → sem correspondente (desativação era irreversível)
 *   alterarSenha  → sem correspondente (senha era imutável após inclusão)
 *   excluir       → sem correspondente (usuário nunca era excluído)
 */
public interface UsuarioService {

    UsuarioResponseDTO incluir(UsuarioRequestDTO dto);

    UsuarioResponseDTO buscarPorId(Long id);

    List<UsuarioResponseDTO> listarTodos();

    List<UsuarioResponseDTO> listarPorPerfil(PerfilUsuario perfil);

    UsuarioResponseDTO alterar(Long id, UsuarioRequestDTO dto);

    /**
     * Altera a senha do usuário validando a senha atual antes de substituir.
     * Lacuna de segurança corrigida: legado gravava senha em texto puro.
     */
    void alterarSenha(Long id, AlterarSenhaDTO dto);

    /**
     * Desativa o usuário (ativo = 'N').
     * Equivale a usuario_desativar() do legado.
     */
    UsuarioResponseDTO desativar(Long id);

    /**
     * Reativa o usuário (ativo = 'S').
     * Lacuna corrigida: desativação era irreversível no legado.
     */
    UsuarioResponseDTO reativar(Long id);

    void excluir(Long id);
}
