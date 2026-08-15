package com.clinic.booking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * Issues and validates JWTs for BOTH patient and admin auth.
 *
 * Token shape:
 * - subject   = accountId (as string) for PATIENT tokens, or the literal
 *               string "ADMIN" for ADMIN tokens (admin has no Account row -
 *               it's a fixed credential pair, not a phone-linked identity).
 * - claim "role" = "PATIENT" or "ADMIN" - this is what JwtAuthenticationFilter
 *               reads to decide which GrantedAuthority to assign, NOT the
 *               subject shape - keeps the two concerns (who vs what-they-can-do) separate.
 */
@Component
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String subject, String role, Map<String, Object> extraClaims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry);

        extraClaims.forEach(builder::claim);

        return builder.signWith(signingKey).compact();
    }

    /** Holds what JwtAuthenticationFilter needs after successfully parsing a token. */
    public record ParsedToken(String subject, String role) {}

    public Optional<ParsedToken> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String role = claims.get("role", String.class);
            return Optional.of(new ParsedToken(claims.getSubject(), role));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}