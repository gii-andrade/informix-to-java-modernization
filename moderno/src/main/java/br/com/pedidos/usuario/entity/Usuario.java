package br.com.pedidos.usuario.entity;

import br.com.pedidos.usuario.enums.PerfilUsuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Entidade que espelha a tabela {@code usuario} do legado Informix.
 *
 * Mapeamento fiel ao schema.sql:
 *   - usuario_id   SERIAL       PK
 *   - login        VARCHAR(50)  NOT NULL UNIQUE  → uq_usuario_login
 *   - nome         VARCHAR(120) NOT NULL
 *   - senha_hash   VARCHAR(255) NOT NULL         → INVISIBLE no usuario.per
 *   - perfil       VARCHAR(20)  NOT NULL DEFAULT 'OPERADOR' → ck_usuario_perfil
 *   - ativo        CHAR(1)      NOT NULL DEFAULT 'S'        → ck_usuario_ativo
 *   - data_cadastro DATE        NOT NULL DEFAULT TODAY
 *
 * Lacunas do legado corrigidas:
 *   • Senha gravada como texto puro no legado (INSERT direto de senha_hash sem hash).
 *     A API aplica BCrypt em runtime antes de persistir — nunca armazena senha em texto claro.
 *   • Não havia tela de login nem controle de sessão — campo perfil existia mas nunca
 *     era verificado em runtime. A entidade agora usa enum PerfilUsuario tipado.
 *   • Não havia função de alterar senha, consultar ou reativar usuário.
 *     A API supre todas essas lacunas.
 */
@Entity
@Table(
    name = "usuario",
    uniqueConstraints = @UniqueConstraint(name = "uq_usuario_login", columnNames = "login")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usuario_id")
    private Long id;

    /**
     * Login único — campo REQUIRED no usuario.per.
     * Constraint uq_usuario_login no schema.
     */
    @Column(name = "login", length = 50, nullable = false)
    private String login;

    /**
     * Nome completo — campo REQUIRED no usuario.per.
     */
    @Column(name = "nome", length = 120, nullable = false)
    private String nome;

    /**
     * Hash BCrypt da senha — campo REQUIRED + INVISIBLE no usuario.per.
     *
     * Lacuna de segurança corrigida: o legado capturava a senha via INPUT BY NAME
     * e a inseria diretamente em senha_hash sem nenhum hash em runtime.
     * Os hashes no seed.sql eram manuais/externos à aplicação.
     * A API aplica BCrypt antes de qualquer persistência.
     *
     * Nunca exposto nas respostas da API (ausente no UsuarioResponseDTO).
     */
    @Column(name = "senha_hash", length = 255, nullable = false)
    private String senhaHash;

    /**
     * Perfil de acesso — DEFAULT 'OPERADOR' no schema.
     * ck_usuario_perfil: ADMIN | OPERADOR | CONSULTA.
     * Armazenado como String para compatibilidade com a coluna VARCHAR(20) legada.
     *
     * Legado: LET perfil = 'OPERADOR' (usuario_incluir, linha 16).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", length = 20, nullable = false)
    @Builder.Default
    private PerfilUsuario perfil = PerfilUsuario.OPERADOR;

    /**
     * Flag de ativação — DEFAULT 'S' no schema.
     * ck_usuario_ativo: apenas 'S' ou 'N'.
     *
     * Legado: usuario_desativar() fazia UPDATE SET ativo='N' (linha 35).
     * Não havia operação de reativar — a API supre essa lacuna.
     */
    @Column(name = "ativo", length = 1, nullable = false)
    @Builder.Default
    private String ativo = "S";

    /**
     * Data de cadastro — equivalente ao DEFAULT TODAY do Informix.
     * Imutável após o INSERT.
     */
    @Column(name = "data_cadastro", nullable = false, updatable = false)
    @Builder.Default
    private LocalDate dataCadastro = LocalDate.now();
}
