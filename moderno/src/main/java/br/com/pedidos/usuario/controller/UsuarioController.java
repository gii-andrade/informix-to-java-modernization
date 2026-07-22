package br.com.pedidos.usuario.controller;

import br.com.pedidos.usuario.dto.AlterarSenhaDTO;
import br.com.pedidos.usuario.dto.UsuarioRequestDTO;
import br.com.pedidos.usuario.dto.UsuarioResponseDTO;
import br.com.pedidos.usuario.enums.PerfilUsuario;
import br.com.pedidos.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controller REST do módulo de Usuários.
 *
 * Mapeamento das funções legadas para endpoints HTTP:
 *
 *   usuario_incluir()   → POST   /usuarios                  → 201 Created
 *   usuario_desativar() → PATCH  /usuarios/{id}/desativar   → 200 OK
 *
 * Operações novas — lacunas do legado corrigidas:
 *   (novo)              → GET    /usuarios                  → 200 OK
 *   (novo)              → GET    /usuarios?perfil=OPERADOR  → 200 OK
 *   (novo)              → GET    /usuarios/{id}             → 200 OK
 *   (novo)              → PUT    /usuarios/{id}             → 200 OK
 *   (novo)              → PATCH  /usuarios/{id}/senha       → 204 No Content
 *   (novo)              → PATCH  /usuarios/{id}/reativar    → 200 OK
 *   (novo)              → DELETE /usuarios/{id}             → 204 No Content
 *
 * Segurança: senhaHash NUNCA aparece nas respostas (ausente no UsuarioResponseDTO).
 */
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService service;

    // -------------------------------------------------------------------------
    // POST /usuarios
    // Equivale a usuario_incluir() do usuario.4gl
    // -------------------------------------------------------------------------
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> incluir(
            @RequestBody @Valid UsuarioRequestDTO dto) {

        UsuarioResponseDTO criado = service.incluir(dto);

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(criado.id())
            .toUri();

        return ResponseEntity.created(location).body(criado);
    }

    // -------------------------------------------------------------------------
    // GET /usuarios  e  GET /usuarios?perfil=ADMIN
    // -------------------------------------------------------------------------
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listar(
            @RequestParam(required = false) PerfilUsuario perfil) {

        List<UsuarioResponseDTO> lista = perfil != null
            ? service.listarPorPerfil(perfil)
            : service.listarTodos();

        return ResponseEntity.ok(lista);
    }

    // -------------------------------------------------------------------------
    // GET /usuarios/{id}
    // -------------------------------------------------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // -------------------------------------------------------------------------
    // PUT /usuarios/{id}
    // Altera login, nome e perfil — senha tem endpoint próprio.
    // Lacuna corrigida: legado não tinha função de alterar dados do usuário.
    // -------------------------------------------------------------------------
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> alterar(
            @PathVariable Long id,
            @RequestBody @Valid UsuarioRequestDTO dto) {

        return ResponseEntity.ok(service.alterar(id, dto));
    }

    // -------------------------------------------------------------------------
    // PATCH /usuarios/{id}/senha
    // Lacuna de segurança corrigida: legado gravava senha em texto puro.
    // Exige senha atual para confirmar identidade antes de trocar.
    // -------------------------------------------------------------------------
    @PatchMapping("/{id}/senha")
    public ResponseEntity<Void> alterarSenha(
            @PathVariable Long id,
            @RequestBody @Valid AlterarSenhaDTO dto) {

        service.alterarSenha(id, dto);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // PATCH /usuarios/{id}/desativar
    // Equivale a usuario_desativar() do usuario.4gl
    // -------------------------------------------------------------------------
    @PatchMapping("/{id}/desativar")
    public ResponseEntity<UsuarioResponseDTO> desativar(@PathVariable Long id) {
        return ResponseEntity.ok(service.desativar(id));
    }

    // -------------------------------------------------------------------------
    // PATCH /usuarios/{id}/reativar
    // Lacuna corrigida: desativação era irreversível no legado.
    // -------------------------------------------------------------------------
    @PatchMapping("/{id}/reativar")
    public ResponseEntity<UsuarioResponseDTO> reativar(@PathVariable Long id) {
        return ResponseEntity.ok(service.reativar(id));
    }

    // -------------------------------------------------------------------------
    // DELETE /usuarios/{id}
    // ATENÇÃO: prefira desativar() para preservar histórico de pedidos.
    // A FK fk_pedido_usuario bloqueia exclusão se houver pedidos vinculados.
    // -------------------------------------------------------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
