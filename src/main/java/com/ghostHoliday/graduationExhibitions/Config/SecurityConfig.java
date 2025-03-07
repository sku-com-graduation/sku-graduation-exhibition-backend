package com.ghostHoliday.graduationExhibitions.Config;

import com.ghostHoliday.graduationExhibitions.utility.JwtAuthenticationFilter;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtility jwtUtility;

    public SecurityConfig(JwtUtility jwtUtility) {
        this.jwtUtility = jwtUtility;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("*")); // 모든 Origin 허용
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")); // 모든 HTTP 메서드 허용
                    config.setAllowedHeaders(List.of("*")); // 모든 헤더 허용
                    config.setAllowCredentials(true);
                    return config;
                }))
                .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // OPTIONS 요청 허용
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN") // ADMIN만 접근 가능
                        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN") // USER 이상 접근 가능
                        .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtility), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
