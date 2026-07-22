package br.com.pedidos.pedido.controller;

import br.com.pedidos.pedido.dto.ItemPedidoRequestDTO;
import br.com.pedidos.pedido.dto.PedidoRequestDTO;
import br.com.pedidos.pedido.dto.PedidoResponseDTO;
import br.com.pedidos.pedido.enums.StatusPedido;
import br.com.pedidos.pedido.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controller REST do módulo de Pedidos.
 *
 * Mapeamento das funções legadas para endpoints HTTP:
 *
 *   pedido_incluir() + pedido_incluir_itens() + pedido_recalcular_total()
 *                           → POST   /pedidos                          → 201 Created
 *   pedido_consultar()      → GET    /pedidos/{id}                     → 200 OK
 *   (novo)                  → GET    /pedidos                          → 200 OK
 *   (novo)                  → GET    /pedidos?status=ABERTO            → 200 OK
 *   (lacuna)                → PATCH  /pedidos/{id}/confirmar           → 200 OK
 *   (lacuna)                → PATCH  /pedidos/{id}/faturar             → 200 OK
 *   pedido_cancelar()       → PATCH  /pedidos/{id}/cancelar            → 200 OK
 *   (novo)                  → POST   /pedidos/{id}/itens               → 200 OK
 *   (novo)                  → DELETE /pedidos/{id}/itens/{produtoId}   → 200 OK
 *   pedido_excluir()        → DELETE /pedidos/{id}                     → 204 No Content
 */
@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService service;

    // -------------------------------------------------------------------------
    // POST /pedidos
    // Cria pedido + itens + calcula total numa única requisição atômica.
    // Equivale a pedido_incluir + pedido_incluir_itens + pedido_recalcular_total.
    // -------------------------------------------------------------------------
    @PostMapping
    public ResponseEntity<PedidoResponseDTO> criar(
            @RequestBody @Valid PedidoRequestDTO dto) {

        PedidoResponseDTO criado = service.criar(dto);

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(criado.id())
            .toUri();

        return ResponseEntity.created(location).body(criado);
    }

    // -------------------------------------------------------------------------
    // GET /pedidos  e  GET /pedidos?status=ABERTO
    // -------------------------------------------------------------------------
    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> listar(
            @RequestParam(required = false) StatusPedido status) {

        List<PedidoResponseDTO> lista = status != null
            ? service.listarPorStatus(status)
            : service.listarTodos();

        return ResponseEntity.ok(lista);
    }

    // -------------------------------------------------------------------------
    // GET /pedidos/{id}
    // Equivale a pedido_consultar() — retorna cabeçalho + itens completos.
    // -------------------------------------------------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // -------------------------------------------------------------------------
    // PATCH /pedidos/{id}/confirmar
    // Transição ABERTO → CONFIRMADO (status existia no schema, sem operação no TUI).
    // -------------------------------------------------------------------------
    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<PedidoResponseDTO> confirmar(@PathVariable Long id) {
        return ResponseEntity.ok(service.confirmar(id));
    }

    // -------------------------------------------------------------------------
    // PATCH /pedidos/{id}/faturar
    // Transição CONFIRMADO → FATURADO (status existia no schema, sem operação no TUI).
    // -------------------------------------------------------------------------
    @PatchMapping("/{id}/faturar")
    public ResponseEntity<PedidoResponseDTO> faturar(@PathVariable Long id) {
        return ResponseEntity.ok(service.faturar(id));
    }

    // -------------------------------------------------------------------------
    // PATCH /pedidos/{id}/cancelar
    // Equivale a pedido_cancelar() + máquina de estados via StatusPedido.
    // -------------------------------------------------------------------------
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<PedidoResponseDTO> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancelar(id));
    }

    // -------------------------------------------------------------------------
    // POST /pedidos/{id}/itens
    // Adiciona item a pedido ABERTO existente.
    // No legado: pedido_incluir_itens() só era chamado na criação.
    // -------------------------------------------------------------------------
    @PostMapping("/{id}/itens")
    public ResponseEntity<PedidoResponseDTO> adicionarItem(
            @PathVariable Long id,
            @RequestBody @Valid ItemPedidoRequestDTO dto) {

        return ResponseEntity.ok(service.adicionarItem(id, dto));
    }

    // -------------------------------------------------------------------------
    // DELETE /pedidos/{id}/itens/{produtoId}
    // Remove item individual de pedido ABERTO.
    // Sem correspondente no legado (exclusão era sempre do pedido inteiro).
    // -------------------------------------------------------------------------
    @DeleteMapping("/{id}/itens/{produtoId}")
    public ResponseEntity<PedidoResponseDTO> removerItem(
            @PathVariable Long id,
            @PathVariable Long produtoId) {

        return ResponseEntity.ok(service.removerItem(id, produtoId));
    }

    // -------------------------------------------------------------------------
    // DELETE /pedidos/{id}
    // Equivale a pedido_excluir() com BEGIN WORK / COMMIT / ROLLBACK WORK.
    // -------------------------------------------------------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
