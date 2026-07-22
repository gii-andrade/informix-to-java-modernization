package br.com.pedidos.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades do JWT lidas de application.properties com o prefixo {@code jwt}.
 *
 * Exemplo em application.properties:
 *   jwt.secret=chave-supersecreta-256bits
 *   jwt.expiration-ms=86400000
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    /** Chave secreta HMAC-SHA256 (mínimo 32 caracteres). */
    String secret,
    /** Validade do token em milissegundos. Padrão: 86400000 (24 h). */
    long expirationMs
) {}
