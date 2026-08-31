package com.ghostHoliday.graduationExhibitions.performance;

import com.ghostHoliday.graduationExhibitions.domain.Account;
import com.ghostHoliday.graduationExhibitions.domain.Category;
import com.ghostHoliday.graduationExhibitions.domain.Post;
import com.ghostHoliday.graduationExhibitions.domain.Professor;
import com.ghostHoliday.graduationExhibitions.domain.Role;
import com.ghostHoliday.graduationExhibitions.domain.Student;
import com.ghostHoliday.graduationExhibitions.domain.StudentProfile;
import com.ghostHoliday.graduationExhibitions.domain.Team;
import com.ghostHoliday.graduationExhibitions.repository.AccountRepository;
import com.ghostHoliday.graduationExhibitions.repository.StudentRepository;
import com.ghostHoliday.graduationExhibitions.repository.TeamRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * N+1 개선 전/후의 <b>응답 시간</b>을 실제 MySQL 로 잰다.
 *
 * <p>{@link ExhibitionListQueryCountTest} 와 {@link ListApiQueryCountTest} 는 쿼리 수를 센다.
 * 쿼리 수는 어디서 재든 같지만 시간은 다르다. 인메모리 H2 에는 네트워크 라운드트립이 없어
 * 쿼리 하나의 비용이 거의 0 이고, 그래서 N+1 의 개선 폭이 실제보다 작게 나온다. 포트폴리오에
 * 쓸 시간 수치는 여기서 뽑는다.
 *
 * <p><b>실행 방법.</b> MySQL 컨테이너가 떠 있어야 한다.
 * <pre>
 * export BENCH_MYSQL_PASSWORD=아무거나
 * docker run -d --name sku-bench-mysql \
 *   -e MYSQL_ROOT_PASSWORD="$BENCH_MYSQL_PASSWORD" -e MYSQL_DATABASE=sku_bench \
 *   -p 3308:3306 mysql:8.0
 *
 * ./gradlew test --tests "*N1LatencyBenchmarkTest" -Dspring.profiles.active=mysql
 * </pre>
 *
 * <p><b>측정 방법.</b> 시나리오마다 개선 전과 개선 후를 번갈아 돌린다. 워밍업 {@value #WARMUP}
 * 회로 InnoDB 버퍼 풀과 JIT 를 데운 뒤 {@value #ROUNDS} 회를 재고 p50 과 p95 를 남긴다.
 * 매 회차 앞에서 영속성 컨텍스트를 비워 1차 캐시가 결과에 섞이지 않게 한다. 두 경로가 같은
 * 값을 돌려주는지도 매번 확인한다. 빨라졌지만 결과가 다르면 의미가 없다.
 */
// showSql = false 로 둔다. @DataJpaTest 기본값이 true 라 측정 로그가 SQL 에 묻힌다.
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("mysql")
// mysql 프로파일 없이 돌면 H2 로 떨어져 시간이 실제보다 짧게 나온다. 그 상태로는 아예 건너뛴다.
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*mysql.*")
class N1LatencyBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(N1LatencyBenchmarkTest.class);

    private static final int YEAR = 2025;
    private static final int WARMUP = 3;
    private static final int ROUNDS = 10;

    @Autowired
    private EntityManager em;

    @Autowired
    private TeamRepository teamRepository;

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
    @DisplayName("전시 목록 - 팀 200개")
    void exhibitionList200() {
        seedTeams(200, true);

        Comparison result = compare(
                () -> collectPostTitles(teamRepository.findAllByExhibitionYear(YEAR)),
                () -> collectPostTitles(teamRepository.findAllWithPostByExhibitionYear(YEAR)));

        log.info(result.report("전시 목록 - 팀 200개 (팀마다 게시글)"));
        assertThat(result.beforeQueries).isEqualTo(201);
        assertThat(result.afterQueries).isEqualTo(1);
    }

    @Test
    @DisplayName("전시 목록 - 팀 40개")
    void exhibitionList40() {
        seedTeams(40, true);

        Comparison result = compare(
                () -> collectPostTitles(teamRepository.findAllByExhibitionYear(YEAR)),
                () -> collectPostTitles(teamRepository.findAllWithPostByExhibitionYear(YEAR)));

        log.info(result.report("전시 목록 - 팀 40개 (팀마다 게시글)"));
        assertThat(result.beforeQueries).isEqualTo(41);
        assertThat(result.afterQueries).isEqualTo(1);
    }

    @Test
    @DisplayName("팀 관리 목록 - 팀 200개 (팀마다 담당 교수)")
    void teamAdminList200() {
        seedTeams(200, true);

        Comparison result = compare(
                () -> collectProfessorNames(teamRepository.findAllByExhibitionYear(YEAR)),
                () -> collectProfessorNames(teamRepository.findAllWithProfessorByExhibitionYear(YEAR)));

        log.info(result.report("팀 관리 목록 - 팀 200개 (팀마다 담당 교수)"));
        assertThat(result.beforeQueries).isEqualTo(201);
        assertThat(result.afterQueries).isEqualTo(1);
    }

    @Test
    @DisplayName("계정 목록 - 계정 30개")
    void accountList30() {
        for (int i = 0; i < 30; i++) {
            Team team = seedTeam("팀" + i, 0, false);
            Account account = new Account();
            account.setTeam(team);
            account.setUserEmail("user" + i + "@test.local");
            account.setRole(Role.MEMBER);
            em.persist(account);
        }
        em.flush();

        Comparison result = compare(
                () -> em.createQuery(
                                "SELECT a FROM Account a WHERE a.role <> 'ADMIN' AND a.team.exhibitionYear = :year",
                                Account.class)
                        .setParameter("year", YEAR)
                        .getResultList().stream()
                        .map(a -> a.getTeam().getName())
                        .sorted()
                        .toList(),
                () -> accountRepository.findNonAdminAccountsByExhibitionYear(YEAR).stream()
                        .map(a -> a.getTeam().getName())
                        .sorted()
                        .toList());

        log.info(result.report("계정 목록 - 계정 30개 (계정마다 소속 팀)"));
        assertThat(result.beforeQueries).isEqualTo(31);
        assertThat(result.afterQueries).isEqualTo(1);
    }

    @Test
    @DisplayName("작품 상세 - 팀원 8명")
    void postDetail8Members() {
        Team team = seedTeam("팀A", 8, false);
        em.flush();
        Long teamId = team.getId();

        Comparison result = compare(
                () -> studentRepository.findAllByTeamId(teamId).stream()
                        .map(s -> s.getStudentProfile().getStudentEmail())
                        .sorted()
                        .toList(),
                () -> studentRepository.findAllByTeamIdWithProfile(teamId).stream()
                        .map(s -> s.getStudentProfile().getStudentEmail())
                        .sorted()
                        .toList());

        log.info(result.report("작품 상세 - 팀원 8명 (팀원마다 프로필)"));
        assertThat(result.beforeQueries).isEqualTo(9);
        assertThat(result.afterQueries).isEqualTo(1);
    }

    // --- 측정 ---------------------------------------------------------------

    /**
     * 개선 전과 개선 후를 번갈아 돌려 비교한다. 번갈아 도는 순서라 측정 도중 머신이 느려져도
     * 한쪽에만 몰리지 않는다.
     */
    private Comparison compare(Supplier<List<String>> before, Supplier<List<String>> after) {
        for (int i = 0; i < WARMUP; i++) {
            runOnce(before);
            runOnce(after);
        }

        long[] beforeNanos = new long[ROUNDS];
        long[] afterNanos = new long[ROUNDS];
        long beforeQueries = 0;
        long afterQueries = 0;
        List<String> beforeResult = null;
        List<String> afterResult = null;

        for (int i = 0; i < ROUNDS; i++) {
            Measurement b = runOnce(before);
            beforeNanos[i] = b.nanos;
            beforeQueries = b.queries;
            beforeResult = b.value;

            Measurement a = runOnce(after);
            afterNanos[i] = a.nanos;
            afterQueries = a.queries;
            afterResult = a.value;
        }

        // 빨라진 것만으로는 부족하다. 두 경로가 같은 값을 내야 한다.
        assertThat(afterResult).isEqualTo(beforeResult);

        return new Comparison(beforeNanos, beforeQueries, afterNanos, afterQueries);
    }

    private Measurement runOnce(Supplier<List<String>> path) {
        em.clear();
        statistics.clear();
        long start = System.nanoTime();
        List<String> value = path.get();
        long elapsed = System.nanoTime() - start;
        return new Measurement(elapsed, statistics.getPrepareStatementCount(), value);
    }

    private record Measurement(long nanos, long queries, List<String> value) {
    }

    private record Comparison(long[] beforeNanos, long beforeQueries,
                              long[] afterNanos, long afterQueries) {

        String report(String title) {
            long beforeP50 = percentile(beforeNanos, 50);
            long beforeP95 = percentile(beforeNanos, 95);
            long afterP50 = percentile(afterNanos, 50);
            long afterP95 = percentile(afterNanos, 95);

            List<String> lines = new ArrayList<>();
            lines.add("");
            lines.add("──────────────────────────────────────────────────────────────");
            lines.add(" " + title);
            lines.add("──────────────────────────────────────────────────────────────");
            lines.add(String.format("  %-10s %10s %12s %12s", "", "쿼리", "p50", "p95"));
            lines.add(String.format("  %-10s %9d회 %12s %12s", "개선 전",
                    beforeQueries, ms(beforeP50), ms(beforeP95)));
            lines.add(String.format("  %-10s %9d회 %12s %12s", "개선 후",
                    afterQueries, ms(afterP50), ms(afterP95)));
            lines.add(String.format("  %-10s %9s  %12s", "감소",
                    String.format("%.0f배", (double) beforeQueries / afterQueries),
                    String.format("%.1f배", (double) beforeP50 / afterP50)));
            lines.add("──────────────────────────────────────────────────────────────");
            lines.add(String.format("  워밍업 %d회 후 %d회 측정. MySQL 8.0, 쿼리 수는 Hibernate 통계의 prepared statement 수.",
                    WARMUP, ROUNDS));
            lines.add("──────────────────────────────────────────────────────────────");
            return String.join(System.lineSeparator(), lines);
        }

        private static String ms(long nanos) {
            return String.format("%.2f ms", nanos / 1_000_000.0);
        }

        private static long percentile(long[] values, int p) {
            long[] sorted = values.clone();
            Arrays.sort(sorted);
            int index = (int) Math.ceil(p / 100.0 * sorted.length) - 1;
            return sorted[Math.max(0, Math.min(index, sorted.length - 1))];
        }
    }

    // --- 목록 화면이 실제로 꺼내 쓰는 값 --------------------------------------

    private List<String> collectPostTitles(List<Team> teams) {
        List<String> titles = new ArrayList<>();
        for (Team team : teams) {
            Post post = team.getPost();
            titles.add(post == null ? null : post.getTitle() + "|" + post.getTeamProfileUrl());
        }
        titles.sort(java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder()));
        return titles;
    }

    private List<String> collectProfessorNames(List<Team> teams) {
        List<String> names = new ArrayList<>();
        for (Team team : teams) {
            Professor professor = team.getProfessor();
            names.add(professor == null ? null : professor.getName());
        }
        names.sort(java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder()));
        return names;
    }

    // --- 데이터 준비 ---------------------------------------------------------

    private void seedTeams(int teamCount, boolean withProfessor) {
        for (int i = 0; i < teamCount; i++) {
            seedTeam("팀 " + i, 0, withProfessor);
        }
        em.flush();
    }

    private Team seedTeam(String name, int studentCount, boolean withProfessor) {
        Post post = new Post();
        post.setTitle(name + " 작품");
        post.setContent("설명");
        post.setTeamProfileUrl("teams/" + name + "/profile.png");
        em.persist(post);

        Team team = new Team();
        team.setName(name);
        team.setExhibitionYear(YEAR);
        team.setCategory(Category.WEB);
        team.setPost(post);

        if (withProfessor) {
            Professor professor = new Professor();
            professor.setName("교수 " + name);
            em.persist(professor);
            team.setProfessor(professor);
        }
        em.persist(team);

        for (int i = 0; i < studentCount; i++) {
            StudentProfile profile = new StudentProfile();
            profile.setStudentEmail(name + "-" + i + "@test.local");
            profile.setInfo("소개");
            em.persist(profile);

            Student student = new Student();
            student.setName(name + " 학생" + i);
            student.setStudentNumber(name + "-" + i);
            student.setRole(Role.MEMBER);
            student.setStudentProfile(profile);
            student.setTeam(team);
            student.setExhibitionYear(String.valueOf(YEAR));
            em.persist(student);
        }
        return team;
    }
}
