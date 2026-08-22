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

    /**
     * 연도별 팀을 게시글과 함께 한 번에 읽는다.
     *
     * <p>목록 화면은 팀마다 게시글(제목·uuid·대표 이미지)을 반드시 꺼내 쓴다. 지연 로딩에
     * 맡기면 팀 수만큼 추가 쿼리가 나가므로 조인해서 가져온다.
     *
     * <p>게시글이 없는 팀도 빠지지 않도록 left join 을 쓴다 — 기존 동작과 같게 유지한다.
     */
    @Query("select t from Team t left join fetch t.post where t.exhibitionYear = :year")
    List<Team> findAllWithPostByExhibitionYear(@Param("year") int year);

    /**
     * 연도별 팀을 담당 교수와 함께 한 번에 읽는다. 담당 교수가 없는 팀도 포함한다.
     */
    @Query("select t from Team t left join fetch t.professor where t.exhibitionYear = :year")
    List<Team> findAllWithProfessorByExhibitionYear(@Param("year") int year);
    public Team findByName(String name);
    Optional<Team> findByPostId(Long id);


    // 해당 교수에 담당된 모든 팀에서 professorId를 NULL로 업데이트
    @Modifying
    @Query("UPDATE Team t SET t.professor = NULL WHERE t.professor.id = :professorId")
    void updateProfessorIdToNull(@Param("professorId") Long professorId);
}
