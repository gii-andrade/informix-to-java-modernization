package br.com.pedidos.security;

import br.com.pedidos.usuario.entity.Usuario;
import br.com.pedidos.usuario.exception.UsuarioInativoException;
import br.com.pedidos.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Endpoint público de autenticação.
 *
 * POST /auth/login
 *   Recebe login + senha, valida credenciais e devolve um JWT.
 *   Este é o único endpoint aberto sem token — todos os demais requerem
 *   {@code Authorization: Bearer <token>}.
 *
 * Perfis e permissões (mapeados de PerfilUsuario):
 *   ADMIN    → acesso total (leitura + escrita em todos os módulos)
 *   OPERADOR → cria/altera clientes, produtos e pedidos; NÃO gerencia usuários
 *   CONSULTA → somente endpoints GET
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder   passwordEncoder;
    private final JwtService        jwtService;

    public record LoginRequest(
        @NotBlank String login,
        @NotBlank @Size(min = 8) String senha
    ) {}

    public record LoginResponse(String token, String perfil, Long expiresInMs) {}

    /**
     * Autentica o usuário e devolve um JWT Bearer.
     *
     * Fluxo:
     *   1. Busca usuário pelo login — 401 se não encontrado
     *   2. Verifica se está ativo — 403 se inativo
     *   3. Compara senha com BCrypt — 401 se incorreta
     *   4. Gera e devolve o token JWT
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody @Valid LoginRequest req) {

        Usuario usuario = usuarioRepository.findByLogin(req.login())
            .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        if ("N".equals(usuario.getAtivo())) {
            throw new UsuarioInativoException(usuario.getId());
        }

        if (!passwordEncoder.matches(req.senha(), usuario.getSenhaHash())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        String token = jwtService.gerarToken(
            usuario.getLogin(),
            usuario.getPerfil().name()
        );

        return ResponseEntity.ok(
            new LoginResponse(token, usuario.getPerfil().name(), 86_400_000L)
        );
    }
}
