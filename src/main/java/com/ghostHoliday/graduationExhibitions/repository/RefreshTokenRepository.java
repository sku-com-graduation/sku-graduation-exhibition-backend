package com.ghostHoliday.graduationExhibitions.repository;

import com.ghostHoliday.graduationExhibitions.domain.Account;
import com.ghostHoliday.graduationExhibitions.domain.RefreshToken;
import com.ghostHoliday.graduationExhibitions.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    // 사용자와 리프레시 토큰을 기반으로 토큰을 조회
    Optional<RefreshToken> findByAccount(Account account);

    // 리프레시 토큰 값으로 토큰을 조회
    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    // 사용자 ID와 리프레시 토큰 값으로 삭제할 수도 있음
    int deleteByAccount(Account account);

}
