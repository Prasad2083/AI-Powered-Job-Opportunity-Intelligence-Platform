package com.jobpulse.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenUtil {

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    @Value("${app.security.jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${app.security.jwt.refresh-expiration}")
    private long refreshTokenExpirationMs;

    @jakarta.annotation.PostConstruct
    void validateConfiguration() {
        if(jwtSecret==null || jwtSecret.getBytes(StandardCharsets.UTF_8).length<64 || jwtSecret.startsWith("replace-") || jwtSecret.startsWith("your-"))
            throw new IllegalStateException("JWT_SECRET must contain at least 64 bytes of generated secret material");
        if(jwtExpirationMs<=0) throw new IllegalStateException("JWT expiration must be positive");
    }

    public String generateToken(String username,String role,long version) {
        return Jwts.builder().subject(username).claim("role",role).claim("tokenVersion",version)
            .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis()+jwtExpirationMs))
            .signWith(getSigningKey(),Jwts.SIG.HS512).compact();
    }
    public long getTokenVersion(String token) {
        Number version=Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload().get("tokenVersion",Number.class);
        return version==null?0:version.longValue();
    }

    public record TokenIdentity(String username,long version) {}
    public java.util.Optional<TokenIdentity> parseIdentity(String token) {
        try {
            Claims claims=Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
            if(claims.getExpiration()==null || claims.getSubject()==null || claims.getSubject().isBlank()) return java.util.Optional.empty();
            Number version=claims.get("tokenVersion",Number.class);
            return java.util.Optional.of(new TokenIdentity(claims.getSubject(),version==null?0:version.longValue()));
        } catch(JwtException | IllegalArgumentException ex) { return java.util.Optional.empty(); }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate JWT token from username and role
     */
    public String generateToken(String username, String role) {
        return createToken(username, role, jwtExpirationMs);
    }

    /**
     * Generate refresh token from username and role
     */
    public String generateRefreshToken(String username, String role) {
        return createToken(username, role, refreshTokenExpirationMs);
    }

    /**
     * Create token with expiration time
     */
    private String createToken(String username, String role, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)  // Add role as a claim
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Get role from token
     */
    public String getRoleFromToken(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    /**
     * Get username from token
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(getSigningKey()).build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
}