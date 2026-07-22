package br.com.pedidos.cliente.repository;

import br.com.pedidos.cliente.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório JPA para a entidade {@link Cliente}.
 *
 * Substitui as queries ESQL embutidas nos blocos SELECT/INSERT/UPDATE/DELETE
 * das funções 4GL cliente_consultar, cliente_incluir, cliente_alterar e
 * cliente_excluir.
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /**
     * Verifica duplicidade de documento antes de incluir ou alterar.
     * Preserva a constraint uq_cliente_documento do schema Informix.
     *
     * Equivale ao controle implícito via SQLCODE != 0 nas funções legadas.
     */
    boolean existsByDocumento(String documento);

    /**
     * Verifica duplicidade de documento excluindo o próprio registro —
     * usado na alteração para permitir que o cliente mantenha seu documento.
     */
    boolean existsByDocumentoAndIdNot(String documento, Long id);

    /**
     * Busca por documento para eventual consulta direta.
     */
    Optional<Cliente> findByDocumento(String documento);
}
