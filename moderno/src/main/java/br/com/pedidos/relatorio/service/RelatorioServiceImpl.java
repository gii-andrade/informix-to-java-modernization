package br.com.pedidos.relatorio.service;

import br.com.pedidos.pedido.repository.PedidoRepository;
import br.com.pedidos.relatorio.dto.RelatorioPedidosStatusDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementação do serviço de relatórios.
 *
 * Porta a função relatorio_pedidos_status() do relatorio.4gl (linhas 8-27),
 * convertendo a exibição em janela de terminal em resposta JSON da API REST.
 *
 * Regras preservadas:
 *   • Agrupa todos os pedidos por status (FOREACH c_status — linha 20)
 *   • Retorna status, quantidade e soma dos valores totais
 *   • Ordenação por status (ORDER BY status — linha 14)
 *   • Valor nulo tratado como zero (COALESCE na query — equivale a linhas 67-69
 *     de pedido_recalcular_total no pedido.4gl)
 */
@Service
@RequiredArgsConstructor
public class RelatorioServiceImpl implements RelatorioService {

    private final PedidoRepository pedidoRepository;

    // =========================================================================
    // relatorio_pedidos_status() — linhas 8-27 do relatorio.4gl
    //
    // Legado: DECLARE c_status CURSOR FOR
    //           SELECT status, COUNT(*), SUM(valor_total)
    //             FROM pedido GROUP BY status ORDER BY status
    //         FOREACH c_status INTO v_status, v_quantidade, v_valor
    //           DISPLAY v_status, v_quantidade, v_valor
    //         END FOREACH
    //
    // API: delega a query ao repositório e retorna a lista como JSON.
    //      @Transactional(readOnly=true) garante snapshot consistente.
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public List<RelatorioPedidosStatusDTO> pedidosPorStatus() {
        return pedidoRepository.relatorioAgrupadoPorStatus();
    }
}
