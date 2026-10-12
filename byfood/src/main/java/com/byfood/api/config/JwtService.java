package com.byfood.api.config;

import com.byfood.api.model.AppUser;
import com.byfood.api.model.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public String generateToken(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getLogin())
                .claim(CLAIM_UID, user.getId())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    /**
     * Valida assinatura e expiração e devolve a identidade do token.
     *
     * @throws io.jsonwebtoken.JwtException       se o token for inválido, adulterado ou expirado
     * @throws IllegalArgumentException           se o token estiver vazio ou sem as claims esperadas
     */
    public AuthUser parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        Number uid = claims.get(CLAIM_UID, Number.class);
        String role = claims.get(CLAIM_ROLE, String.class);
        if (uid == null || role == null) {
            throw new IllegalArgumentException("Token sem identidade");
        }
        return new AuthUser(uid.longValue(), claims.getSubject(), UserRole.valueOf(role));
    }
}
