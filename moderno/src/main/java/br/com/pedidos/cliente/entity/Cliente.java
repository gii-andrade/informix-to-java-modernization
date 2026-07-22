package br.com.pedidos.cliente.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

/**
 * Entidade que espelha a tabela {@code cliente} do legado Informix.
 *
 * Mapeamento fiel ao schema.sql:
 *   - razao_social  VARCHAR(150) NOT NULL
 *   - nome_fantasia VARCHAR(150)
 *   - documento     VARCHAR(20)  NOT NULL UNIQUE  → regra RN: unicidade de CNPJ/CPF
 *   - email         VARCHAR(120)
 *   - telefone      VARCHAR(30)
 *   - endereco      VARCHAR(200)
 *   - cidade        VARCHAR(80)
 *   - uf            CHAR(2)
 *   - cep           VARCHAR(10)
 *   - ativo         CHAR(1)      NOT NULL DEFAULT 'S'  → regra RN: só 'S' ou 'N'
 *   - data_cadastro DATE         NOT NULL DEFAULT TODAY
 *
 * Campos omitidos no formulário .per original (nome_fantasia, telefone,
 * endereco, cidade, uf, cep) são preservados aqui para não perder dados.
 */
@Entity
@Table(
    name = "cliente",
    uniqueConstraints = @UniqueConstraint(name = "uq_cliente_documento", columnNames = "documento")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cliente_id")
    private Long id;

    /** Obrigatório — campo REQUIRED no cliente.per */
    @Column(name = "razao_social", length = 150, nullable = false)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 150)
    private String nomeFantasia;

    /** Obrigatório e único — constraint uq_cliente_documento + campo REQUIRED no .per */
    @Column(name = "documento", length = 20, nullable = false)
    private String documento;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "telefone", length = 30)
    private String telefone;

    @Column(name = "endereco", length = 200)
    private String endereco;

    @Column(name = "cidade", length = 80)
    private String cidade;

    @Column(name = "uf", length = 2)
    private String uf;

    @Column(name = "cep", length = 10)
    private String cep;

    /**
     * Legado usa CHAR(1): 'S' = ativo, 'N' = inativo.
     * Regra de negócio: novo cliente começa como ativo ('S').
     * Excluir cliente com pedidos é bloqueado pela FK fk_pedido_cliente.
     */
    @Column(name = "ativo", length = 1, nullable = false)
    @Builder.Default
    private String ativo = "S";

    /**
     * Preenchido automaticamente na criação — equivalente ao DEFAULT TODAY do Informix.
     * Imutável após o INSERT.
     */
    @Column(name = "data_cadastro", nullable = false, updatable = false)
    @Builder.Default
    private LocalDate dataCadastro = LocalDate.now();
}
