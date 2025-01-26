package com.ghostHoliday.graduationExhibitions.utility;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtUtility {

    // 안전한 SecretKey 자동 생성
    private final SecretKey secretKey;

    public JwtUtility() {
        this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }


    private static final long EXPIRATION_TIME = 1000L * 60 * 60; // 1시간

    // 토큰 생성 메서드
    public String generateToken(String userEmail) {
        return Jwts.builder()
                .setSubject(userEmail)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(secretKey) // 안전한 SecretKey 사용
                .compact();
    }

    // 토큰 검증 메서드
    public Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey) // 검증 시 동일한 SecretKey 사용
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException ex) {
            throw new IllegalArgumentException("Invalid JWT token", ex);
        }
    }

    public boolean isTokenExpired(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.before(new Date());
    }
}
