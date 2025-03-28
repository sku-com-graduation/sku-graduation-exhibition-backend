package com.ghostHoliday.graduationExhibitions.utility;

import com.ghostHoliday.graduationExhibitions.domain.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Service
public class JwtUtility {

    private final SecretKey secretKey;

    public JwtUtility() {
        this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }

    // ⏳ Access Token 만료 시간 (30분)
    private static final long ACCESS_TOKEN_EXPIRATION = 1000L * 60 * 2; // 2분

    // ⏳ Refresh Token 만료 시간 (24시간)
    private static final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60; // 1시간

    // 토큰 생성 (role 포함)
    public String generateToken(String userEmail, Role role) {
        return Jwts.builder()
                .setSubject(userEmail)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION)) // 30분
                .addClaims(Map.of("role", role.name()))
                .signWith(secretKey)
                .compact();
    }

    // 토큰 검증 및 파싱
    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token.replace("Bearer ", ""))
                .getBody();

        return claims.getSubject(); // 이메일 반환
    }

    // 토큰 만료 여부 확인
    public boolean isTokenExpired(String token) {
        return validateToken(token).getExpiration().before(new Date());
    }

    // Refresh Token 생성
    public String generateRefreshToken(String userEmail) {
        return Jwts.builder()
                .setSubject(userEmail)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION)) // 24시간
                .signWith(secretKey)
                .compact();
    }



}
