package za.co.digitalcowboy.agents.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {
    
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    
    private final SecretKey secretKey;
    private final long accessTokenExpiryMinutes;
    private final long refreshTokenExpiryDays;
    
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry-minutes:30}") long accessTokenExpiryMinutes,
            @Value("${jwt.refresh-token-expiry-days:7}") long refreshTokenExpiryDays) {
        
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT secret cannot be null or empty");
        }
        
        this.secretKey = createSecretKey(secret);
        this.accessTokenExpiryMinutes = accessTokenExpiryMinutes;
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
    }
    
    private SecretKey createSecretKey(String secret) {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(secret);
            return Keys.hmacShaKeyFor(decodedKey);
        } catch (Exception e) {
            log.warn("JWT secret not base64 encoded, using as-is. For production, use a proper base64 encoded key.");
            return Keys.hmacShaKeyFor(secret.getBytes());
        }
    }
    
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("typ", "access");
        claims.put("roles", userDetails.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .toList());
        
        return createToken(claims, userDetails.getUsername(), accessTokenExpiryMinutes, ChronoUnit.MINUTES);
    }
    
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("typ", "refresh");
        
        return createToken(claims, userDetails.getUsername(), refreshTokenExpiryDays, ChronoUnit.DAYS);
    }
    
    private String createToken(Map<String, Object> claims, String subject, long expiry, ChronoUnit unit) {
        Instant now = Instant.now();
        Instant expiryTime = now.plus(expiry, unit);
        
        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiryTime))
            .signWith(secretKey)
            .compact();
    }
    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("typ", String.class));
    }
    
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (Exception e) {
            log.debug("Error parsing JWT token: {}", e.getMessage());
            throw e;
        }
    }
    
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    public boolean isAccessToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            return "access".equals(tokenType);
        } catch (Exception e) {
            log.debug("Error checking token type: {}", e.getMessage());
            return false;
        }
    }
    
    public boolean isRefreshToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            return "refresh".equals(tokenType);
        } catch (Exception e) {
            log.debug("Error checking token type: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            log.debug("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }
    
    public long getAccessTokenExpirySeconds() {
        return accessTokenExpiryMinutes * 60;
    }
}