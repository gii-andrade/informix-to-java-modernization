package br.com.pedidos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Geração e validação de tokens JWT.
 *
 * O subject do token é o login do usuário; o claim {@code perfil} carrega
 * o valor de {@link br.com.pedidos.usuario.enums.PerfilUsuario} para
 * autorização baseada em roles.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;

    /** Gera um token assinado com HMAC-SHA256 para o login e perfil informados. */
    public String gerarToken(String login, String perfil) {
        Date agora   = new Date();
        Date expira  = new Date(agora.getTime() + props.expirationMs());

        return Jwts.builder()
            .subject(login)
            .claim("perfil", perfil)
            .issuedAt(agora)
            .expiration(expira)
            .signWith(chave())
            .compact();
    }

    /** Extrai o login (subject) de um token válido. */
    public String extrairLogin(String token) {
        return claims(token).getSubject();
    }

    /** Extrai o perfil do claim customizado. */
    public String extrairPerfil(String token) {
        return claims(token).get("perfil", String.class);
    }

    /**
     * Valida assinatura e expiração do token.
     * Lança {@link io.jsonwebtoken.JwtException} se inválido.
     */
    public boolean validar(String token) {
        try {
            claims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------

    private Claims claims(String token) {
        return Jwts.parser()
            .verifyWith(chave())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey chave() {
        return Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }
}
