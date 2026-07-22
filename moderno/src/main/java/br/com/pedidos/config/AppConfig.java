package br.com.pedidos.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuração central de beans de infraestrutura da aplicação.
 *
 * O bean PasswordEncoder (BCrypt) foi movido para SecurityConfig para
 * ficar junto das demais configurações de segurança.
 */
@Configuration
public class AppConfig {
    // Beans de infraestrutura adicionais podem ser declarados aqui.
}
