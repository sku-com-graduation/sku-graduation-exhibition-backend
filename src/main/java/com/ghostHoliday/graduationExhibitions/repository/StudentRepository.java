package com.ghostHoliday.graduationExhibitions.repository;

import com.ghostHoliday.graduationExhibitions.domain.Student;
import com.ghostHoliday.graduationExhibitions.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentRepository extends JpaRepository <Student, Long> {

    // studentNumber를 기준으로 학생을 찾는 메서드
    Student findByStudentNumber(String studentNumber);

    // exhibitionYear를 기준으로 학생 리스트를 반환
    List<Student> findByExhibitionYear(String exhibitionYear);

    List<Student> findAllByTeamId(Long TeamId);
    List<Student> findByTeamIsNull();

    /**
     * 팀에 속한 학생을 프로필과 함께 읽는다.
     *
     * <p>작품 상세·편집 화면은 학생마다 소개·깃허브·이메일·프로필 이미지를 전부 꺼내 쓴다.
     * 지연 로딩에 맡기면 학생 수만큼 쿼리가 더 나간다.
     */
    @Query("select s from Student s left join fetch s.studentProfile where s.team.id = :teamId")
    List<Student> findAllByTeamIdWithProfile(@Param("teamId") Long teamId);

    /** 아직 팀이 없는 학생을 프로필과 함께 읽는다 — 계정 생성 화면의 후보 목록. */
    @Query("select s from Student s left join fetch s.studentProfile where s.team is null")
    List<Student> findByTeamIsNullWithProfile();

    /** 삭제 대상 학생을 프로필과 함께 읽는다 — 프로필 이미지를 지우려면 프로필이 필요하다. */
    @Query("select s from Student s left join fetch s.studentProfile where s.id in :ids")
    List<Student> findAllByIdWithProfile(@Param("ids") List<Long> ids);
}
