package com.ghostHoliday.graduationExhibitions.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class JwtUtilityTest {
    private final JwtUtility jwtUtility = new JwtUtility();

    @Test
    void generateToken() {
        String userEmail = "juns0720@sungkyul.ac.kr";
        String token = jwtUtility.generateToken(userEmail);
        Claims claims = jwtUtility.validateToken(token);


        assertNotNull(token);
        assertNotNull(claims);
        assertEquals(userEmail, claims.getSubject());
    }

}