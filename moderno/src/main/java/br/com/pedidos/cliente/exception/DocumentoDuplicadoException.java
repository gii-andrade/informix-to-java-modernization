package br.com.pedidos.cliente.exception;

/**
 * Lançada quando se tenta incluir ou alterar um cliente com documento
 * já cadastrado para outro registro.
 *
 * Equivale ao erro do legado:
 *   ERROR "Erro ao incluir cliente."  (quando SQLCODE != 0 por violação de
 *   uq_cliente_documento na função cliente_incluir / cliente_alterar)
 */
public class DocumentoDuplicadoException extends RuntimeException {

    public DocumentoDuplicadoException(String documento) {
        super("Já existe um cliente cadastrado com o documento: " + documento);
    }
}
