package com.ghostHoliday.graduationExhibitions.repository;


import com.ghostHoliday.graduationExhibitions.domain.Post;
import com.ghostHoliday.graduationExhibitions.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    public List<Team> findAllByExhibitionYear(int year);
    public Team findByName(String name);
    Optional<Team> findByPostId(Long id);


    // 해당 교수에 담당된 모든 팀에서 professorId를 NULL로 업데이트
    @Modifying
    @Query("UPDATE Team t SET t.professor = NULL WHERE t.professor.id = :professorId")
    void updateProfessorIdToNull(@Param("professorId") Long professorId);
}
