package br.com.pedidos.usuario.exception;

/**
 * Lançada quando um usuário não é encontrado pelo ID informado.
 *
 * Equivale ao erro do legado:
 *   ERROR "Usuario nao encontrado."  (usuario_desativar — linha 39 do usuario.4gl)
 */
public class UsuarioNaoEncontradoException extends RuntimeException {

    public UsuarioNaoEncontradoException(Long id) {
        super("Usuário não encontrado: id=" + id);
    }
}
