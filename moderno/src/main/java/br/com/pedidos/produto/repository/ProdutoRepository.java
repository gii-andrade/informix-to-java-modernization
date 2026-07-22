package br.com.pedidos.produto.repository;

import br.com.pedidos.produto.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório JPA para a entidade {@link Produto}.
 *
 * Substitui as queries ESQL embutidas em produto_incluir(), produto_consultar()
 * e produto_excluir() do produto.4gl.
 *
 * Métodos derivados por convenção de nome — nenhum SQL manual necessário:
 *
 *   existsByCodigo              → verifica unicidade antes do INSERT (produto_incluir)
 *   existsByCodigoAndIdNot      → verifica unicidade na alteração sem rejeitar o próprio registro
 *   findByCodigo                → consulta alternativa por código de negócio
 */
@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    /**
     * Verifica duplicidade de código antes de incluir.
     * Preserva a constraint uq_produto_codigo do schema Informix.
     * Equivale ao controle implícito via SQLCODE != 0 na função produto_incluir().
     */
    boolean existsByCodigo(String codigo);

    /**
     * Verifica duplicidade de código excluindo o próprio registro —
     * usado na alteração (produto_alterar — lacuna suprida pela API REST).
     */
    boolean existsByCodigoAndIdNot(String codigo, Long id);

    /**
     * Busca por código de negócio para consulta direta sem precisar do ID interno.
     */
    Optional<Produto> findByCodigo(String codigo);
}
