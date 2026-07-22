package br.com.pedidos.cliente.controller;

import br.com.pedidos.cliente.dto.ClienteRequestDTO;
import br.com.pedidos.cliente.dto.ClienteResponseDTO;
import br.com.pedidos.cliente.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controller REST do módulo de Clientes.
 *
 * Mapeamento das funções legadas para endpoints HTTP:
 *
 *   cliente_incluir()   → POST   /clientes          → 201 Created
 *   cliente_consultar() → GET    /clientes/{id}     → 200 OK
 *   listarTodos()       → GET    /clientes           → 200 OK  (novo)
 *   cliente_alterar()   → PUT    /clientes/{id}     → 200 OK
 *   cliente_excluir()   → DELETE /clientes/{id}     → 204 No Content
 */
@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService service;

    // -------------------------------------------------------------------------
    // POST /clientes
    // Equivale a cliente_incluir() do cliente.4gl
    // -------------------------------------------------------------------------
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> incluir(
            @RequestBody @Valid ClienteRequestDTO dto) {

        ClienteResponseDTO criado = service.incluir(dto);

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(criado.id())
            .toUri();

        return ResponseEntity.created(location).body(criado);
    }

    // -------------------------------------------------------------------------
    // GET /clientes
    // Listagem completa — sem correspondente no legado
    // -------------------------------------------------------------------------
    @GetMapping
    public ResponseEntity<List<ClienteResponseDTO>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    // -------------------------------------------------------------------------
    // GET /clientes/{id}
    // Equivale a cliente_consultar() do cliente.4gl
    // -------------------------------------------------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // -------------------------------------------------------------------------
    // PUT /clientes/{id}
    // Equivale a cliente_alterar() do cliente.4gl
    // -------------------------------------------------------------------------
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> alterar(
            @PathVariable Long id,
            @RequestBody @Valid ClienteRequestDTO dto) {

        return ResponseEntity.ok(service.alterar(id, dto));
    }

    // -------------------------------------------------------------------------
    // DELETE /clientes/{id}
    // Equivale a cliente_excluir() do cliente.4gl
    // Retorna 204 No Content em caso de sucesso.
    // Retorna 409 Conflict se cliente possuir pedidos (via GlobalExceptionHandler).
    // -------------------------------------------------------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
