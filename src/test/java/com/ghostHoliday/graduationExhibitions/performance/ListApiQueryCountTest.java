package com.ghostHoliday.graduationExhibitions.performance;

import com.ghostHoliday.graduationExhibitions.domain.Account;
import com.ghostHoliday.graduationExhibitions.domain.Category;
import com.ghostHoliday.graduationExhibitions.domain.Post;
import com.ghostHoliday.graduationExhibitions.domain.Role;
import com.ghostHoliday.graduationExhibitions.domain.Student;
import com.ghostHoliday.graduationExhibitions.domain.StudentProfile;
import com.ghostHoliday.graduationExhibitions.domain.Team;
import com.ghostHoliday.graduationExhibitions.repository.AccountRepository;
import com.ghostHoliday.graduationExhibitions.repository.StudentRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 목록·상세 API 들이 지연 로딩 때문에 내던 추가 쿼리를 개선 전/후로 비교한다.
 *
 * <p>{@link ExhibitionListQueryCountTest} 가 전시 목록 하나를 다뤘다면, 여기서는 같은 패턴이
 * 남아 있던 나머지 화면들을 확인한다 — 작품 상세(학생 프로필)와 계정 목록(소속 팀).
 *
 * <p>학생 목록도 같은 모양으로 보였지만 재보니 N+1 이 아니었다. 지연 로딩 프록시는 식별자를
 * 이미 들고 있어서, 코드가 {@code getId()} 만 꺼내면 초기화되지 않기 때문이다.
 * 그 경로에는 조인을 넣지 않았다 — 얻는 것 없이 조인 비용만 늘기 때문이다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ListApiQueryCountTest {

    private static final int YEAR = 2025;

    @Autowired
    private EntityManager em;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    @Test
    @DisplayName("작품 상세의 팀원 목록 — 팀원 8명: 개선 전 9쿼리, 개선 후 1쿼리")
    void postDetailStudents() {
        Team team = seedTeam("팀A", 8);
        em.flush();
        em.clear();

        statistics.clear();
        List<String> legacy = studentRepository.findAllByTeamId(team.getId()).stream()
                .map(s -> s.getStudentProfile().getStudentEmail())
                .toList();
        long legacyQueries = statistics.getPrepareStatementCount();

        em.clear();

        statistics.clear();
        List<String> improved = studentRepository.findAllByTeamIdWithProfile(team.getId()).stream()
                .map(s -> s.getStudentProfile().getStudentEmail())
                .toList();
        long improvedQueries = statistics.getPrepareStatementCount();

        print("작품 상세 — 팀원 8명 프로필", legacyQueries, improvedQueries);

        assertThat(improved).isEqualTo(legacy);
        assertThat(legacyQueries).isEqualTo(1 + 8);
        assertThat(improvedQueries).isEqualTo(1);
    }

    @Test
    @DisplayName("계정 목록 — 계정 30개: 개선 전 31쿼리, 개선 후 1쿼리")
    void accountListJoinsTeam() {
        for (int i = 0; i < 30; i++) {
            Team team = seedTeam("팀" + i, 0);
            Account account = new Account();
            account.setTeam(team);
            account.setUserEmail("user" + i + "@test.local");
            account.setRole(Role.MEMBER);
            em.persist(account);
        }
        em.flush();
        em.clear();

        // 개선 전 쿼리 — 조인 없이 계정만 읽고, 팀 이름은 루프에서 꺼냈다
        statistics.clear();
        List<String> legacy = em.createQuery(
                        "SELECT a FROM Account a WHERE a.role <> 'ADMIN' AND a.team.exhibitionYear = :year",
                        Account.class)
                .setParameter("year", YEAR)
                .getResultList().stream()
                .map(a -> a.getTeam().getName())
                .toList();
        long legacyQueries = statistics.getPrepareStatementCount();

        em.clear();

        statistics.clear();
        List<String> improved = accountRepository.findNonAdminAccountsByExhibitionYear(YEAR).stream()
                .map(a -> a.getTeam().getName())
                .toList();
        long improvedQueries = statistics.getPrepareStatementCount();

        print("계정 목록 — 30개 + 팀 이름", legacyQueries, improvedQueries);

        assertThat(improved).containsExactlyInAnyOrderElementsOf(legacy);
        assertThat(legacyQueries).isEqualTo(1 + 30);
        assertThat(improvedQueries).isEqualTo(1);
    }

    @Test
    @DisplayName("학생 목록은 원래 N+1 이 아니었다 — 지연 프록시에서 id 만 읽으면 쿼리가 없다")
    void studentListWasNeverNPlusOne() {
        seedTeam("팀A", 50);
        em.flush();
        em.clear();

        statistics.clear();
        List<Long> teamIds = studentRepository.findByExhibitionYear(String.valueOf(YEAR)).stream()
                .map(s -> s.getTeam() == null ? null : s.getTeam().getId())
                .toList();
        long queries = statistics.getPrepareStatementCount();

        System.out.printf("%n  학생 50명 + 소속 팀 id — 쿼리 %d회 (조인 없이도 1회)%n", queries);

        assertThat(teamIds).hasSize(50);
        // 지연 로딩 프록시는 식별자를 이미 들고 있어서 getId() 만으로는 초기화되지 않는다.
        // 그래서 여기에는 조인을 넣지 않았다 — 넣어도 얻는 게 없고 조인 비용만 는다.
        assertThat(queries).isEqualTo(1);
    }

    @Test
    @DisplayName("팀이 없는 학생도 목록에서 빠지지 않는다")
    void studentsWithoutTeamAreStillListed() {
        seedTeam("팀A", 3);
        Student loner = newStudent("무소속", "9999");
        loner.setExhibitionYear(String.valueOf(YEAR));
        em.persist(loner);
        em.flush();
        em.clear();

        List<Student> students = studentRepository.findByExhibitionYear(String.valueOf(YEAR));

        assertThat(students).hasSize(4);
        assertThat(students.stream().filter(s -> s.getTeam() == null)).hasSize(1);
    }

    // --- 데이터 준비 ---------------------------------------------------------

    private Team seedTeam(String name, int studentCount) {
        Post post = new Post();
        post.setTitle(name + " 작품");
        em.persist(post);

        Team team = new Team();
        team.setName(name);
        team.setExhibitionYear(YEAR);
        team.setCategory(Category.WEB);
        team.setPost(post);
        em.persist(team);

        for (int i = 0; i < studentCount; i++) {
            Student student = newStudent(name + " 학생" + i, name + "-" + i);
            student.setTeam(team);
            student.setExhibitionYear(String.valueOf(YEAR));
            em.persist(student);
        }
        return team;
    }

    private Student newStudent(String name, String studentNumber) {
        StudentProfile profile = new StudentProfile();
        profile.setStudentEmail(studentNumber + "@test.local");
        profile.setInfo("소개");
        em.persist(profile);

        Student student = new Student();
        student.setName(name);
        student.setStudentNumber(studentNumber);
        student.setRole(Role.MEMBER);
        student.setStudentProfile(profile);
        return student;
    }

    private static void print(String title, long legacyQueries, long improvedQueries) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add("─────────────────────────────────────────────");
        lines.add(" " + title);
        lines.add(String.format("  개선 전 쿼리 %d회  →  개선 후 %d회", legacyQueries, improvedQueries));
        lines.add("─────────────────────────────────────────────");
        System.out.println(String.join(System.lineSeparator(), lines));
    }
}
