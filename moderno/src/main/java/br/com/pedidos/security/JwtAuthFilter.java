package br.com.pedidos.security;

import br.com.pedidos.usuario.entity.Usuario;
import br.com.pedidos.usuario.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro JWT que intercepta cada requisição, valida o Bearer token e
 * popula o {@link SecurityContextHolder} com o usuário autenticado.
 *
 * Fluxo:
 *   1. Extrai o header {@code Authorization: Bearer <token>}
 *   2. Valida assinatura e expiração via {@link JwtService}
 *   3. Carrega o usuário do banco e verifica se está ativo
 *   4. Define a autenticação no contexto Spring Security
 *
 * Requisições sem token (ex.: POST /auth/login) seguem adiante sem autenticação;
 * o {@link SecurityConfig} decide se o endpoint é público ou protegido.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService       jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (!jwtService.validar(token)) {
            chain.doFilter(request, response);
            return;
        }

        String login  = jwtService.extrairLogin(token);
        String perfil = jwtService.extrairPerfil(token);

        // Verifica se o usuário ainda existe e está ativo
        Usuario usuario = usuarioRepository.findByLogin(login).orElse(null);
        if (usuario == null || "N".equals(usuario.getAtivo())) {
            chain.doFilter(request, response);
            return;
        }

        // ROLE_ é o prefixo padrão do Spring Security para hasRole()
        var auth = new UsernamePasswordAuthenticationToken(
            login,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + perfil))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(request, response);
    }
}
