package br.com.pedidos.usuario.exception;

/**
 * Lançada quando se tenta realizar uma operação que exige usuário ativo
 * (ex.: alterar dados ou senha de usuário desativado).
 *
 * Lacuna corrigida: o legado desativava usuários mas nunca os reativava
 * nem impedia operações sobre usuários inativos.
 */
public class UsuarioInativoException extends RuntimeException {

    public UsuarioInativoException(Long id) {
        super("Usuário id=" + id + " está inativo. Reative-o antes de realizar esta operação.");
    }
}
