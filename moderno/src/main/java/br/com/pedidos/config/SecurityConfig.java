package br.com.pedidos.config;

import br.com.pedidos.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração de segurança da aplicação.
 *
 * Política de autorização por perfil (espelha PerfilUsuario):
 *
 *   ADMIN    → acesso total a todos os endpoints
 *   OPERADOR → GET + escrita em clientes, produtos, pedidos e relatórios
 *              NÃO pode gerenciar /usuarios (exceto alterar própria senha)
 *   CONSULTA → somente GETs em todos os módulos
 *
 * O endpoint POST /auth/login é público (sem token).
 * Os endpoints do Actuator (/actuator/health, /actuator/info) são públicos.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Sem estado — API REST stateless com JWT
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth

                // ── Público ──────────────────────────────────────────────────
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                // ── Relatórios — todos os perfis autenticados ─────────────────
                .requestMatchers(HttpMethod.GET, "/relatorios/**").authenticated()

                // ── Usuários — somente ADMIN ──────────────────────────────────
                .requestMatchers("/usuarios/**").hasRole("ADMIN")

                // ── Clientes ──────────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,  "/clientes/**").authenticated()
                .requestMatchers("/clientes/**").hasAnyRole("ADMIN", "OPERADOR")

                // ── Produtos ──────────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,  "/produtos/**").authenticated()
                .requestMatchers("/produtos/**").hasAnyRole("ADMIN", "OPERADOR")

                // ── Pedidos ───────────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,  "/pedidos/**").authenticated()
                .requestMatchers("/pedidos/**").hasAnyRole("ADMIN", "OPERADOR")

                // Qualquer outra rota requer autenticação
                .anyRequest().authenticated()
            )

            // Adiciona o filtro JWT antes do filtro padrão de usuário/senha
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCryptPasswordEncoder — mantido aqui para que o Spring Security o reconheça
     * como o encoder padrão da cadeia de autenticação. Substitui o bean em AppConfig.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
