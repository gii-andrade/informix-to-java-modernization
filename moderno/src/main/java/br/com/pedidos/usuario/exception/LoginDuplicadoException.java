package br.com.pedidos.usuario.exception;

/**
 * Lançada quando se tenta incluir ou alterar um usuário com login
 * já cadastrado para outro registro.
 *
 * Equivale ao erro do legado:
 *   ERROR "Erro ao incluir usuario."  (usuario_incluir — linha 26 do usuario.4gl)
 *   — gerado quando SQLCODE != 0 por violação de uq_usuario_login.
 */
public class LoginDuplicadoException extends RuntimeException {

    public LoginDuplicadoException(String login) {
        super("Já existe um usuário cadastrado com o login: " + login);
    }
}
