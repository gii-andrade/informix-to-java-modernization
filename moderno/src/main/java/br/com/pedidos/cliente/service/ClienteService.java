package br.com.pedidos.cliente.service;

import br.com.pedidos.cliente.dto.ClienteRequestDTO;
import br.com.pedidos.cliente.dto.ClienteResponseDTO;

import java.util.List;

/**
 * Contrato do serviço de clientes.
 *
 * As operações espelham exatamente as funções do legado cliente.4gl:
 *   incluir   → cliente_incluir()
 *   buscarPorId  → cliente_consultar()
 *   alterar   → cliente_alterar()
 *   excluir   → cliente_excluir()
 *
 * listarTodos é uma adição natural da API REST, sem correspondente no legado.
 */
public interface ClienteService {

    ClienteResponseDTO incluir(ClienteRequestDTO dto);

    ClienteResponseDTO buscarPorId(Long id);

    List<ClienteResponseDTO> listarTodos();

    ClienteResponseDTO alterar(Long id, ClienteRequestDTO dto);

    void excluir(Long id);
}
