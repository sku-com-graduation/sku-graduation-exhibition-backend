package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtUtilityTest {

    @Autowired
    private JwtUtility jwtUtility;

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