package dasein.sem1pks.service;

import dasein.sem1pks.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    private final Key signingKey;
    private final long expirationMilliseconds;

    public JwtService(JwtProperties properties) {
        byte[] keyBytes = properties.accessSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT access secret must contain at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMilliseconds = properties.accessExpirationMilliseconds();
    }

    public String generateToken(Long userId, String email, String role, boolean isBlocked) {
        Date now = new Date();
        Date expDate = new Date(now.getTime() + expirationMilliseconds);

        return Jwts.builder()
                .setSubject(email)
                .claim("id", userId)
                .claim("role", role)
                .claim("is_blocked", isBlocked)
                .setIssuedAt(now)
                .setExpiration(expDate)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public TokenData parseToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Long userId = claims.get("id", Long.class);
        String email = claims.getSubject();
        String role = claims.get("role", String.class);
        Boolean isBlocked = claims.get("is_blocked", Boolean.class);
        if (userId == null || email == null || role == null || isBlocked == null) {
            throw new IllegalArgumentException("JWT does not contain required claims");
        }

        return new TokenData(userId, email, role, isBlocked);
    }

    public record TokenData(Long userId, String email, String role, boolean isBlocked) {}
}
