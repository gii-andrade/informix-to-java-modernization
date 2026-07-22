package br.com.pedidos.cliente.service;

import br.com.pedidos.cliente.dto.ClienteRequestDTO;
import br.com.pedidos.cliente.dto.ClienteResponseDTO;
import br.com.pedidos.cliente.entity.Cliente;
import br.com.pedidos.cliente.exception.ClienteNaoEncontradoException;
import br.com.pedidos.cliente.exception.ClientePossuiPedidosException;
import br.com.pedidos.cliente.exception.DocumentoDuplicadoException;
import br.com.pedidos.cliente.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Implementação do serviço de clientes.
 *
 * Cada método mapeia diretamente uma função do legado cliente.4gl,
 * preservando todas as regras de negócio identificadas no relatório técnico.
 */
@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository repository;

    // -------------------------------------------------------------------------
    // cliente_incluir() — linha 11 do cliente.4gl
    // Regras preservadas:
    //   • razaoSocial e documento são obrigatórios (REQUIRED no .per)
    //   • ativo inicia como 'S' (LET ativo = 'S', linha 18)
    //   • documento deve ser único (uq_cliente_documento)
    //   • data_cadastro = hoje (DEFAULT TODAY do Informix)
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public ClienteResponseDTO incluir(ClienteRequestDTO dto) {
        if (repository.existsByDocumento(dto.documento())) {
            throw new DocumentoDuplicadoException(dto.documento());
        }

        Cliente cliente = Cliente.builder()
            .razaoSocial(dto.razaoSocial())
            .nomeFantasia(dto.nomeFantasia())
            .documento(dto.documento())
            .email(dto.email())
            .telefone(dto.telefone())
            .endereco(dto.endereco())
            .cidade(dto.cidade())
            .uf(dto.uf())
            .cep(dto.cep())
            // Se ativo não informado, aplica 'S' como padrão (LET ativo = 'S' no legado)
            .ativo(dto.ativo() != null ? dto.ativo() : "S")
            .dataCadastro(LocalDate.now())
            .build();

        return ClienteResponseDTO.from(repository.save(cliente));
    }

    // -------------------------------------------------------------------------
    // cliente_consultar() — linha 34 do cliente.4gl
    // Regra preservada: retorna erro se ID não existe
    // -------------------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Long id) {
        Cliente cliente = repository.findById(id)
            .orElseThrow(() -> new ClienteNaoEncontradoException(id));
        return ClienteResponseDTO.from(cliente);
    }

    // -------------------------------------------------------------------------
    // Adição: listagem completa (sem correspondente no legado TUI)
    // -------------------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listarTodos() {
        return repository.findAll()
            .stream()
            .map(ClienteResponseDTO::from)
            .toList();
    }

    // -------------------------------------------------------------------------
    // cliente_alterar() — linha 54 do cliente.4gl
    // Regras preservadas:
    //   • cliente deve existir (ERROR "Cliente nao encontrado.")
    //   • documento mantém unicidade — mas o cliente pode manter o próprio
    //   • campos alteráveis: razaoSocial, documento, email, ativo
    //   • Expansão: campos extras do schema agora editáveis (antes inacessíveis no TUI)
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public ClienteResponseDTO alterar(Long id, ClienteRequestDTO dto) {
        Cliente cliente = repository.findById(id)
            .orElseThrow(() -> new ClienteNaoEncontradoException(id));

        if (repository.existsByDocumentoAndIdNot(dto.documento(), id)) {
            throw new DocumentoDuplicadoException(dto.documento());
        }

        cliente.setRazaoSocial(dto.razaoSocial());
        cliente.setNomeFantasia(dto.nomeFantasia());
        cliente.setDocumento(dto.documento());
        cliente.setEmail(dto.email());
        cliente.setTelefone(dto.telefone());
        cliente.setEndereco(dto.endereco());
        cliente.setCidade(dto.cidade());
        cliente.setUf(dto.uf());
        cliente.setCep(dto.cep());
        cliente.setAtivo(dto.ativo() != null ? dto.ativo() : cliente.getAtivo());

        return ClienteResponseDTO.from(repository.save(cliente));
    }

    // -------------------------------------------------------------------------
    // cliente_excluir() — linha 84 do cliente.4gl
    // Regra preservada:
    //   • Excluir cliente que possui pedidos é bloqueado.
    //   • No legado: violação da FK fk_pedido_cliente resultava em SQLCODE != 0
    //     e exibia "Cliente possui pedidos ou nao existe."
    //   • Aqui: a FK no banco ainda protege; capturamos DataIntegrityViolationException
    //     e relançamos como exceção de domínio legível.
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public void excluir(Long id) {
        if (!repository.existsById(id)) {
            throw new ClienteNaoEncontradoException(id);
        }
        try {
            repository.deleteById(id);
            repository.flush(); // força o SQL antes de sair da transação
        } catch (DataIntegrityViolationException ex) {
            throw new ClientePossuiPedidosException(id);
        }
    }
}
