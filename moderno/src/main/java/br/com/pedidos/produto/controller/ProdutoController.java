package br.com.pedidos.produto.controller;

import br.com.pedidos.produto.dto.ProdutoRequestDTO;
import br.com.pedidos.produto.dto.ProdutoResponseDTO;
import br.com.pedidos.produto.service.ProdutoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

/**
 * Controller REST do módulo de Produtos.
 *
 * Mapeamento das funções legadas para endpoints HTTP:
 *
 *   produto_incluir()   → POST   /produtos              → 201 Created
 *   produto_consultar() → GET    /produtos/{id}         → 200 OK
 *   listarTodos()       → GET    /produtos              → 200 OK  (novo)
 *   produto_alterar()   → PUT    /produtos/{id}         → 200 OK  (lacuna corrigida)
 *   alterarPreco()      → PATCH  /produtos/{id}/preco   → 200 OK  (operação especializada)
 *   ajustarEstoque()    → PATCH  /produtos/{id}/estoque → 200 OK  (operação especializada)
 *   produto_excluir()   → DELETE /produtos/{id}         → 204 No Content
 */
@RestController
@RequestMapping("/produtos")
@RequiredArgsConstructor
@Validated
public class ProdutoController {

    private final ProdutoService service;

    // -------------------------------------------------------------------------
    // POST /produtos
    // Equivale a produto_incluir() do produto.4gl
    // -------------------------------------------------------------------------
    @PostMapping
    public ResponseEntity<ProdutoResponseDTO> incluir(
            @RequestBody @Valid ProdutoRequestDTO dto) {

        ProdutoResponseDTO criado = service.incluir(dto);

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(criado.id())
            .toUri();

        return ResponseEntity.created(location).body(criado);
    }

    // -------------------------------------------------------------------------
    // GET /produtos
    // Listagem completa — sem correspondente no legado
    // -------------------------------------------------------------------------
    @GetMapping
    public ResponseEntity<List<ProdutoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    // -------------------------------------------------------------------------
    // GET /produtos/{id}
    // Equivale a produto_consultar() do produto.4gl
    // -------------------------------------------------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // -------------------------------------------------------------------------
    // PUT /produtos/{id}
    // Supre a lacuna de produto_alterar() que não existia no legado.
    // Substitui todos os campos do produto (full update).
    // -------------------------------------------------------------------------
    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> alterar(
            @PathVariable Long id,
            @RequestBody @Valid ProdutoRequestDTO dto) {

        return ResponseEntity.ok(service.alterar(id, dto));
    }

    // -------------------------------------------------------------------------
    // PATCH /produtos/{id}/preco
    // Atualização especializada de preço de venda.
    // Motivação: o legado não tinha como alterar preço sem excluir/recriar.
    // ck_produto_preco: precoVenda >= 0.
    // -------------------------------------------------------------------------
    @PatchMapping("/{id}/preco")
    public ResponseEntity<ProdutoResponseDTO> alterarPreco(
            @PathVariable Long id,
            @RequestParam
            @DecimalMin(value = "0.00", message = "Preço de venda não pode ser negativo")
            @Digits(integer = 10, fraction = 2, message = "Preço inválido")
            BigDecimal valor) {

        return ResponseEntity.ok(service.alterarPreco(id, valor));
    }

    // -------------------------------------------------------------------------
    // PATCH /produtos/{id}/estoque
    // Ajuste de estoque atual.
    // Motivação: o legado definia estoque apenas na inclusão.
    // ck_produto_estoque: estoqueAtual >= 0.
    // -------------------------------------------------------------------------
    @PatchMapping("/{id}/estoque")
    public ResponseEntity<ProdutoResponseDTO> ajustarEstoque(
            @PathVariable Long id,
            @RequestParam
            @DecimalMin(value = "0.000", message = "Estoque não pode ser negativo")
            @Digits(integer = 9, fraction = 3, message = "Estoque inválido")
            BigDecimal valor) {

        return ResponseEntity.ok(service.ajustarEstoque(id, valor));
    }

    // -------------------------------------------------------------------------
    // DELETE /produtos/{id}
    // Equivale a produto_excluir() do produto.4gl
    // 204 No Content em caso de sucesso.
    // 409 Conflict se produto possuir itens (via GlobalExceptionHandler).
    // -------------------------------------------------------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
