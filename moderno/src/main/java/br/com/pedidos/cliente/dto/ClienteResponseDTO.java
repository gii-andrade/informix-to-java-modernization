package br.com.pedidos.cliente.dto;

import br.com.pedidos.cliente.entity.Cliente;

import java.time.LocalDate;

/**
 * DTO de saída retornado nas respostas da API.
 *
 * Expõe todos os campos da tabela {@code cliente}, incluindo os que o
 * formulário legado cliente.per omitia (nomeFantasia, telefone, endereco,
 * cidade, uf, cep) — dados existentes no banco mas inacessíveis pela TUI.
 */
public record ClienteResponseDTO(
    Long id,
    String razaoSocial,
    String nomeFantasia,
    String documento,
    String email,
    String telefone,
    String endereco,
    String cidade,
    String uf,
    String cep,
    String ativo,
    LocalDate dataCadastro
) {

    /** Converte uma entidade {@link Cliente} para o DTO de resposta. */
    public static ClienteResponseDTO from(Cliente cliente) {
        return new ClienteResponseDTO(
            cliente.getId(),
            cliente.getRazaoSocial(),
            cliente.getNomeFantasia(),
            cliente.getDocumento(),
            cliente.getEmail(),
            cliente.getTelefone(),
            cliente.getEndereco(),
            cliente.getCidade(),
            cliente.getUf(),
            cliente.getCep(),
            cliente.getAtivo(),
            cliente.getDataCadastro()
        );
    }
}
