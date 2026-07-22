package br.com.pedidos.usuario.repository;

import br.com.pedidos.usuario.entity.Usuario;
import br.com.pedidos.usuario.enums.PerfilUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório JPA para a entidade {@link Usuario}.
 *
 * Substitui as queries ESQL embutidas em usuario_incluir() e usuario_desativar()
 * do usuario.4gl.
 *
 * Métodos derivados por convenção de nome — nenhum SQL manual necessário.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Verifica duplicidade de login antes de incluir.
     * Preserva a constraint uq_usuario_login do schema Informix.
     * Equivale ao controle implícito via SQLCODE != 0 em usuario_incluir().
     */
    boolean existsByLogin(String login);

    /**
     * Verifica duplicidade de login excluindo o próprio registro —
     * usado na alteração para permitir que o usuário mantenha seu login.
     */
    boolean existsByLoginAndIdNot(String login, Long id);

    /**
     * Busca por login — base para autenticação futura e busca direta.
     */
    Optional<Usuario> findByLogin(String login);

    /**
     * Lista usuários por perfil — sem correspondente na TUI legada.
     */
    List<Usuario> findByPerfil(PerfilUsuario perfil);

    /**
     * Lista usuários pelo flag ativo — para relatórios e listagens filtradas.
     */
    List<Usuario> findByAtivo(String ativo);
}
