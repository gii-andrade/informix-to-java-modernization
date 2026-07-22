package br.com.pedidos.cliente.service;

import br.com.pedidos.cliente.dto.ClienteRequestDTO;
import br.com.pedidos.cliente.dto.ClienteResponseDTO;
import br.com.pedidos.cliente.entity.Cliente;
import br.com.pedidos.cliente.exception.ClienteNaoEncontradoException;
import br.com.pedidos.cliente.exception.ClientePossuiPedidosException;
import br.com.pedidos.cliente.exception.DocumentoDuplicadoException;
import br.com.pedidos.cliente.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de ClienteServiceImpl.
 *
 * Cobre os cenários mapeados nas regras de negócio do legado:
 *   RN-incluir-ok     → cliente_incluir() com sucesso
 *   RN-incluir-dup    → cliente_incluir() com documento duplicado
 *   RN-consultar-ok   → cliente_consultar() com sucesso
 *   RN-consultar-nf   → cliente_consultar() cliente não encontrado
 *   RN-alterar-ok     → cliente_alterar() com sucesso
 *   RN-alterar-dup    → cliente_alterar() com documento de outro cliente
 *   RN-excluir-ok     → cliente_excluir() com sucesso
 *   RN-excluir-nf     → cliente_excluir() cliente não existe
 *   RN-excluir-fk     → cliente_excluir() cliente com pedidos (violação FK)
 *   RN-ativo-default  → ativo = 'S' quando não informado (LET ativo = 'S' no legado)
 */
@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository repository;

    @InjectMocks
    private ClienteServiceImpl service;

    private Cliente clienteSalvo;

    @BeforeEach
    void setUp() {
        clienteSalvo = Cliente.builder()
            .id(1L)
            .razaoSocial("Almeida & Filhos Ltda.")
            .documento("12.345.678/0001-90")
            .email("compras@almeida.com.br")
            .ativo("S")
            .dataCadastro(LocalDate.now())
            .build();
    }

    // --- incluir ---

    @Test
    @DisplayName("incluir: deve criar cliente com ativo='S' por padrão quando não informado")
    void incluir_ativoDefault() {
        ClienteRequestDTO dto = new ClienteRequestDTO(
            "Almeida & Filhos Ltda.", null,
            "12.345.678/0001-90", null, null, null, null, null, null, null
        );
        when(repository.existsByDocumento(dto.documento())).thenReturn(false);
        when(repository.save(any())).thenReturn(clienteSalvo);

        ClienteResponseDTO resp = service.incluir(dto);

        assertThat(resp.ativo()).isEqualTo("S");
        verify(repository).save(argThat(c -> "S".equals(c.getAtivo())));
    }

    @Test
    @DisplayName("incluir: deve lançar DocumentoDuplicadoException se documento já existir")
    void incluir_documentoDuplicado() {
        ClienteRequestDTO dto = new ClienteRequestDTO(
            "Outra Empresa Ltda.", null,
            "12.345.678/0001-90", null, null, null, null, null, null, "S"
        );
        when(repository.existsByDocumento(dto.documento())).thenReturn(true);

        assertThatThrownBy(() -> service.incluir(dto))
            .isInstanceOf(DocumentoDuplicadoException.class)
            .hasMessageContaining("12.345.678/0001-90");

        verify(repository, never()).save(any());
    }

    // --- buscarPorId ---

    @Test
    @DisplayName("buscarPorId: deve retornar cliente existente")
    void buscarPorId_encontrado() {
        when(repository.findById(1L)).thenReturn(Optional.of(clienteSalvo));

        ClienteResponseDTO resp = service.buscarPorId(1L);

        assertThat(resp.id()).isEqualTo(1L);
        assertThat(resp.razaoSocial()).isEqualTo("Almeida & Filhos Ltda.");
    }

    @Test
    @DisplayName("buscarPorId: deve lançar ClienteNaoEncontradoException para ID inexistente")
    void buscarPorId_naoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
            .isInstanceOf(ClienteNaoEncontradoException.class)
            .hasMessageContaining("99");
    }

    // --- alterar ---

    @Test
    @DisplayName("alterar: deve atualizar campos editáveis preservando dataCadastro")
    void alterar_sucesso() {
        ClienteRequestDTO dto = new ClienteRequestDTO(
            "Almeida & Filhos Ltda. Atualizada", null,
            "12.345.678/0001-90", "novo@email.com", null, null, null, null, null, "N"
        );
        when(repository.findById(1L)).thenReturn(Optional.of(clienteSalvo));
        when(repository.existsByDocumentoAndIdNot(dto.documento(), 1L)).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponseDTO resp = service.alterar(1L, dto);

        assertThat(resp.razaoSocial()).isEqualTo("Almeida & Filhos Ltda. Atualizada");
        assertThat(resp.ativo()).isEqualTo("N");
        // data_cadastro nunca muda (updatable = false)
        assertThat(resp.dataCadastro()).isEqualTo(clienteSalvo.getDataCadastro());
    }

    @Test
    @DisplayName("alterar: deve lançar DocumentoDuplicadoException se novo documento pertence a outro cliente")
    void alterar_documentoDeOutroCliente() {
        ClienteRequestDTO dto = new ClienteRequestDTO(
            "Almeida", null, "99.999.999/0001-99", null, null, null, null, null, null, "S"
        );
        when(repository.findById(1L)).thenReturn(Optional.of(clienteSalvo));
        when(repository.existsByDocumentoAndIdNot("99.999.999/0001-99", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.alterar(1L, dto))
            .isInstanceOf(DocumentoDuplicadoException.class);
    }

    // --- excluir ---

    @Test
    @DisplayName("excluir: deve remover cliente sem pedidos com sucesso")
    void excluir_sucesso() {
        when(repository.existsById(1L)).thenReturn(true);
        doNothing().when(repository).deleteById(1L);

        assertThatCode(() -> service.excluir(1L)).doesNotThrowAnyException();
        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("excluir: deve lançar ClienteNaoEncontradoException para ID inexistente")
    void excluir_naoEncontrado() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.excluir(99L))
            .isInstanceOf(ClienteNaoEncontradoException.class);
    }

    @Test
    @DisplayName("excluir: deve lançar ClientePossuiPedidosException quando FK é violada")
    void excluir_clienteComPedidos() {
        when(repository.existsById(1L)).thenReturn(true);
        doThrow(DataIntegrityViolationException.class).when(repository).deleteById(1L);

        assertThatThrownBy(() -> service.excluir(1L))
            .isInstanceOf(ClientePossuiPedidosException.class)
            .hasMessageContaining("1");
    }
}
