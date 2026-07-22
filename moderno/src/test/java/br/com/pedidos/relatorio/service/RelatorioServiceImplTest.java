package br.com.pedidos.relatorio.service;

import br.com.pedidos.pedido.enums.StatusPedido;
import br.com.pedidos.pedido.repository.PedidoRepository;
import br.com.pedidos.relatorio.dto.RelatorioPedidosStatusDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de RelatorioServiceImpl.
 *
 * Cenários rastreados ao legado relatorio.4gl:
 *
 *   pedidosPorStatus():
 *     • comPedidos     → FOREACH c_status retorna linhas (linhas 20-24)
 *     • semPedidos     → FOREACH não executa, retorna lista vazia
 */
@ExtendWith(MockitoExtension.class)
class RelatorioServiceImplTest {

    @Mock
    PedidoRepository pedidoRepository;

    @InjectMocks
    RelatorioServiceImpl service;

    // =========================================================================
    // pedidosPorStatus() — relatorio_pedidos_status() do relatorio.4gl
    // =========================================================================

    @Test
    @DisplayName("pedidosPorStatus: deve retornar lista com dados agrupados quando há pedidos")
    void pedidosPorStatus_comPedidos_retornaLista() {
        List<RelatorioPedidosStatusDTO> esperado = List.of(
            new RelatorioPedidosStatusDTO(StatusPedido.ABERTO,     2L, new BigDecimal("300.00")),
            new RelatorioPedidosStatusDTO(StatusPedido.CANCELADO,  1L, new BigDecimal("150.00")),
            new RelatorioPedidosStatusDTO(StatusPedido.CONFIRMADO, 1L, new BigDecimal("200.00")),
            new RelatorioPedidosStatusDTO(StatusPedido.FATURADO,   3L, new BigDecimal("750.00"))
        );

        when(pedidoRepository.relatorioAgrupadoPorStatus()).thenReturn(esperado);

        List<RelatorioPedidosStatusDTO> resultado = service.pedidosPorStatus();

        assertThat(resultado).hasSize(4);
        assertThat(resultado.get(0).status()).isEqualTo(StatusPedido.ABERTO);
        assertThat(resultado.get(0).quantidade()).isEqualTo(2L);
        assertThat(resultado.get(0).valorTotal()).isEqualByComparingTo("300.00");
        assertThat(resultado.get(3).status()).isEqualTo(StatusPedido.FATURADO);
        assertThat(resultado.get(3).quantidade()).isEqualTo(3L);
        assertThat(resultado.get(3).valorTotal()).isEqualByComparingTo("750.00");

        verify(pedidoRepository, times(1)).relatorioAgrupadoPorStatus();
    }

    @Test
    @DisplayName("pedidosPorStatus: deve retornar lista vazia quando não há pedidos")
    void pedidosPorStatus_semPedidos_retornaListaVazia() {
        when(pedidoRepository.relatorioAgrupadoPorStatus()).thenReturn(List.of());

        List<RelatorioPedidosStatusDTO> resultado = service.pedidosPorStatus();

        assertThat(resultado).isEmpty();
        verify(pedidoRepository, times(1)).relatorioAgrupadoPorStatus();
    }
}
