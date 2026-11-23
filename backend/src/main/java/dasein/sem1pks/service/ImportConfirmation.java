package dasein.sem1pks.service;

import dasein.sem1pks.config.JwtProperties;
import dasein.sem1pks.exception.conflict.CatalogConflictException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

@Component
class ImportConfirmation {
    private final Key key;

    ImportConfirmation(JwtProperties properties) {
        key = Keys.hmacShaKeyFor(digest(("csv-import:" + properties.accessSecret()).getBytes(StandardCharsets.UTF_8)));
    }

    String issue(UUID actor, String fingerprint) {
        return Jwts.builder().setId(UUID.randomUUID().toString()).setSubject(actor.toString())
                .setExpiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .claim("fingerprint", fingerprint).signWith(key, SignatureAlgorithm.HS256).compact();
    }

    UUID verify(UUID actor, String token, String fingerprint) {
        try {
            var claims = Jwts.parserBuilder().setSigningKey(key).requireSubject(actor.toString())
                    .require("fingerprint", fingerprint).build().parseClaimsJws(token).getBody();
            return UUID.fromString(claims.getId());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new CatalogConflictException("Файл или данные изменились либо проверка истекла. Проверьте CSV заново");
        }
    }

    static String hash(byte[] bytes) {
        return HexFormat.of().formatHex(digest(bytes));
    }

    private static byte[] digest(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
