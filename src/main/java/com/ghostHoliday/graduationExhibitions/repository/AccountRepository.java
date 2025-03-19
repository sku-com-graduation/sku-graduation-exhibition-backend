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


    @Query("SELECT a FROM Account a WHERE a.role <> 'ADMIN' AND a.team.exhibitionYear = :year")
    List<Account> findNonAdminAccountsByExhibitionYear(@Param("year") int exhibitionYear);

    boolean existsAccountByUserEmail(String userEmail);



}
