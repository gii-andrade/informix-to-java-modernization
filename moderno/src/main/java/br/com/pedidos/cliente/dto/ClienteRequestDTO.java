package br.com.pedidos.cliente.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

/**
 * DTO de entrada para criação e atualização de clientes.
 *
 * Validações mapeadas a partir do legado:
 *  - razaoSocial  → campo REQUIRED no cliente.per + NOT NULL no schema
 *  - documento    → campo REQUIRED no cliente.per + UNIQUE (uq_cliente_documento)
 *  - ativo        → constraint ck_cliente_ativo: apenas 'S' ou 'N'
 *  - email        → opcional no .per, mas validado como e-mail se informado
 *  - uf           → exatamente 2 caracteres (CHAR(2) no schema)
 */
@Builder
public record ClienteRequestDTO(

    @NotBlank(message = "Razão social é obrigatória")
    @Size(max = 150, message = "Razão social deve ter no máximo 150 caracteres")
    String razaoSocial,

    @Size(max = 150, message = "Nome fantasia deve ter no máximo 150 caracteres")
    String nomeFantasia,

    @NotBlank(message = "Documento é obrigatório")
    @Size(max = 20, message = "Documento deve ter no máximo 20 caracteres")
    String documento,

    @Email(message = "E-mail inválido")
    @Size(max = 120, message = "E-mail deve ter no máximo 120 caracteres")
    String email,

    @Size(max = 30, message = "Telefone deve ter no máximo 30 caracteres")
    String telefone,

    @Size(max = 200, message = "Endereço deve ter no máximo 200 caracteres")
    String endereco,

    @Size(max = 80, message = "Cidade deve ter no máximo 80 caracteres")
    String cidade,

    @Size(min = 2, max = 2, message = "UF deve ter exatamente 2 caracteres")
    String uf,

    @Size(max = 10, message = "CEP deve ter no máximo 10 caracteres")
    String cep,

    /**
     * Regra de negócio: ativo só aceita 'S' ou 'N' — espelho de ck_cliente_ativo.
     * Se omitido na criação o service aplica o default 'S', como no legado:
     *   LET ativo = 'S'  (cliente_incluir, linha 18 do cliente.4gl)
     */
    @Pattern(regexp = "[SN]", message = "Ativo deve ser 'S' (sim) ou 'N' (não)")
    String ativo

) {}
