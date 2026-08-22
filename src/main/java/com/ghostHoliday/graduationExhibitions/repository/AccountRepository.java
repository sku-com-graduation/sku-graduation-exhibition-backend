package com.ghostHoliday.graduationExhibitions.repository;


import com.ghostHoliday.graduationExhibitions.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findAccountByUserEmail(String userEmail);

    @Modifying
    @Query("UPDATE Account a SET a.pwd = a.defaultPwd WHERE a.id IN :ids")
    void resetPasswordsToDefault(@Param("ids") List<Long> ids);

    void deleteByTeamId(Long teamId);


    /**
     * 연도별 비관리자 계정을 소속 팀과 함께 읽는다.
     *
     * <p>계정 목록 화면은 계정마다 팀 이름을 꺼내 쓴다. 지연 로딩에 맡기면 계정 수만큼
     * 쿼리가 더 나갔다. 어차피 팀 조건으로 거르는 쿼리라 조인을 명시하고 함께 읽는다.
     */
    @Query("SELECT a FROM Account a JOIN FETCH a.team t WHERE a.role <> 'ADMIN' AND t.exhibitionYear = :year")
    List<Account> findNonAdminAccountsByExhibitionYear(@Param("year") int exhibitionYear);

    boolean existsAccountByUserEmail(String userEmail);



}
