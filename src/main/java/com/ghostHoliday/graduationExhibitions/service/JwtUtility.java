package com.ghostHoliday.graduationExhibitions.service;

import io.jsonwebtoken.*;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtUtility {
    private String secret = "yourSecretKey";

    private static final long EXPIRATION_TIME = 1000L * 60 * 60;

    // 토큰 생성
    public String generateToken(String userId) {
        return Jwts.builder() // Jwts.builder()로 JWT 생성
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS512, secret.getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    // 토큰 검증
    public Claims validateToken(String token) {
        try {
            // Jwts.parserBuilder()로 파서 생성
            JwtParser parser = Jwts.parserBuilder()
                    .setSigningKey(secret.getBytes(StandardCharsets.UTF_8))
                    .build();

            // 토큰을 파싱하여 Claims 반환
            Claims claims = parser.parseClaimsJws(token).getBody();
            return claims;
        } catch (JwtException ex) {
            // JWT 오류가 발생하면 예외를 던짐
            throw ex;
        }
    }
}
