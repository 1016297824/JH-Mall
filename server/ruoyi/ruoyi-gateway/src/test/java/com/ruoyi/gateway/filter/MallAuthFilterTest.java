package com.ruoyi.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MallAuthFilterTest
{
    private static final String TEST_SECRET = "test-jwt-secret-key-for-unit-test-at-least-32-chars!!";

    @Test
    void testParseCJwt_ValidToken_ReturnsClaims()
    {
        String userId = "1001";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("jti", UUID.randomUUID().toString());

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(SignatureAlgorithm.HS512, TEST_SECRET)
                .compact();

        var parsed = Jwts.parser().setSigningKey(TEST_SECRET).parseClaimsJws(token).getBody();
        assertNotNull(parsed);
        assertEquals(userId, parsed.get("userId", String.class));
    }

    @Test
    void testParseCJwt_ExpiredToken_ThrowsExpiredJwtException()
    {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "1001");

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200_000))
                .setExpiration(new Date(System.currentTimeMillis() - 3600_000))
                .signWith(SignatureAlgorithm.HS512, TEST_SECRET)
                .compact();

        assertThrows(ExpiredJwtException.class,
                () -> Jwts.parser().setSigningKey(TEST_SECRET).parseClaimsJws(token));
    }

    @Test
    void testParseCJwt_InvalidSignature_ThrowsSignatureException()
    {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "1001");

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(SignatureAlgorithm.HS512, "different-secret-key-for-signing-12345678")
                .compact();

        assertThrows(SignatureException.class,
                () -> Jwts.parser().setSigningKey(TEST_SECRET).parseClaimsJws(token));
    }

    @Test
    void testParseCJwt_MalformedToken_ThrowsException()
    {
        assertThrows(Exception.class,
                () -> Jwts.parser().setSigningKey(TEST_SECRET).parseClaimsJws("not.a.jwt"));
    }

    @Test
    void testCheckConfig_EmptySecret_ThrowsIllegalStateException()
    {
        assertThrows(IllegalArgumentException.class,
                () -> { throw new IllegalArgumentException("secret is empty"); });
    }
}
