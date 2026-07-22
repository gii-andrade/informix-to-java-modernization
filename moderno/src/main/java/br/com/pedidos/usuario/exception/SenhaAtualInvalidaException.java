package br.com.pedidos.usuario.exception;

/**
 * Lançada quando a senha atual informada não confere com o hash armazenado
 * durante a operação de alteração de senha.
 *
 * Lacuna corrigida: o legado não tinha operação de alteração de senha,
 * portanto essa exceção não existia. Criada para proteger o endpoint
 * PATCH /usuarios/{id}/senha.
 */
public class SenhaAtualInvalidaException extends RuntimeException {

    public SenhaAtualInvalidaException() {
        super("A senha atual informada está incorreta.");
    }
}
