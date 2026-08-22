package com.ghostHoliday.graduationExhibitions.performance;

import com.ghostHoliday.graduationExhibitions.domain.Category;
import com.ghostHoliday.graduationExhibitions.domain.Post;
import com.ghostHoliday.graduationExhibitions.domain.Professor;
import com.ghostHoliday.graduationExhibitions.domain.Team;
import com.ghostHoliday.graduationExhibitions.repository.TeamRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 연도별 전시 목록 조회의 쿼리 수를 개선 전/후로 나란히 잰다.
 *
 * <p>목록 화면은 팀을 가져온 뒤 팀마다 게시글(제목·uuid·대표 이미지)을 꺼내 쓴다. 지연 로딩에
 * 맡기면 팀 수만큼 추가 쿼리가 나갔다. 조인으로 한 번에 읽도록 바꾼 뒤의 차이를 여기서 확인한다.
 *
 * <p>쿼리 수는 Hibernate 통계의 prepared statement 수로 센다 — 실제로 DB에 나간 SQL 개수다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ExhibitionListQueryCountTest {

    private static final Logger log = LoggerFactory.getLogger(ExhibitionListQueryCountTest.class);

    private static final int YEAR = 2025;

    @Autowired
    private EntityManager em;

    @Autowired
    private TeamRepository teamRepository;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    @Test
    @DisplayName("전시 목록 — 팀 40개: 개선 전 41쿼리, 개선 후 1쿼리")
    void exhibitionList_40teams() {
        Result result = measurePostList(40);

        log.info(result.report("전시 목록 (게시글 조인) — 팀 40개"));

        assertThat(result.legacyTitles).isEqualTo(result.improvedTitles);
        assertThat(result.legacyQueries).isEqualTo(1 + 40);
        assertThat(result.improvedQueries).isEqualTo(1);
    }

    @Test
    @DisplayName("전시 목록 — 팀 200개: 개선 전 201쿼리, 개선 후 1쿼리")
    void exhibitionList_200teams() {
        Result result = measurePostList(200);

        log.info(result.report("전시 목록 (게시글 조인) — 팀 200개"));

        assertThat(result.legacyTitles).isEqualTo(result.improvedTitles);
        assertThat(result.legacyQueries).isEqualTo(1 + 200);
        assertThat(result.improvedQueries).isEqualTo(1);
    }

    @Test
    @DisplayName("팀 관리 목록 (교수 조인) — 팀 40개: 개선 전 41쿼리, 개선 후 1쿼리")
    void teamAdminList_40teams() {
        seed(40, true);
        em.flush();
        em.clear();

        statistics.clear();
        List<String> legacy = teamRepository.findAllByExhibitionYear(YEAR).stream()
                .map(team -> team.getProfessor() == null ? null : team.getProfessor().getName())
                .toList();
        long legacyQueries = statistics.getPrepareStatementCount();

        em.clear();

        statistics.clear();
        List<String> improved = teamRepository.findAllWithProfessorByExhibitionYear(YEAR).stream()
                .map(team -> team.getProfessor() == null ? null : team.getProfessor().getName())
                .toList();
        long improvedQueries = statistics.getPrepareStatementCount();

        log.info(String.format(
                "%n팀 관리 목록 (교수 조인) — 팀 40개%n  개선 전 쿼리 %d회 / 개선 후 쿼리 %d회%n",
                legacyQueries, improvedQueries));

        assertThat(legacy).isEqualTo(improved);
        assertThat(legacyQueries).isEqualTo(1 + 40);
        assertThat(improvedQueries).isEqualTo(1);
    }

    @Test
    @DisplayName("게시글이 없는 팀도 목록에서 빠지지 않는다")
    void teamsWithoutPostAreStillListed() {
        List<Team> teams = seed(5, false);
        teams.get(2).setPost(null);
        em.flush();
        em.clear();

        List<Team> loaded = teamRepository.findAllWithPostByExhibitionYear(YEAR);

        assertThat(loaded).hasSize(5);
        assertThat(loaded.stream().filter(team -> team.getPost() == null)).hasSize(1);
    }

    // --- 측정 ---------------------------------------------------------------

    private Result measurePostList(int teamCount) {
        seed(teamCount, true);
        em.flush();
        em.clear();

        statistics.clear();
        long legacyStart = System.nanoTime();
        List<String> legacyTitles = collectTitles(teamRepository.findAllByExhibitionYear(YEAR));
        long legacyNanos = System.nanoTime() - legacyStart;
        long legacyQueries = statistics.getPrepareStatementCount();

        em.clear();

        statistics.clear();
        long improvedStart = System.nanoTime();
        List<String> improvedTitles = collectTitles(teamRepository.findAllWithPostByExhibitionYear(YEAR));
        long improvedNanos = System.nanoTime() - improvedStart;
        long improvedQueries = statistics.getPrepareStatementCount();

        return new Result(legacyTitles, legacyQueries, legacyNanos,
                improvedTitles, improvedQueries, improvedNanos);
    }

    /** 목록 화면이 실제로 꺼내 쓰는 값 — 게시글 제목과 대표 이미지 경로. */
    private List<String> collectTitles(List<Team> teams) {
        List<String> titles = new ArrayList<>();
        for (Team team : teams) {
            Post post = team.getPost();
            titles.add(post == null ? null : post.getTitle() + "|" + post.getTeamProfileUrl());
        }
        return titles;
    }

    // --- 데이터 준비 ---------------------------------------------------------

    private List<Team> seed(int teamCount, boolean withProfessor) {
        List<Team> teams = new ArrayList<>();

        for (int i = 0; i < teamCount; i++) {
            Post post = new Post();
            post.setTitle("작품 " + i);
            post.setContent("설명 " + i);
            post.setTeamProfileUrl("teams/" + i + "/profile.png");
            em.persist(post);

            Team team = new Team();
            team.setName("팀 " + i);
            team.setExhibitionYear(YEAR);
            team.setCategory(Category.values()[i % Category.values().length]);
            team.setPost(post);

            if (withProfessor) {
                Professor professor = new Professor();
                professor.setName("교수 " + i);
                em.persist(professor);
                team.setProfessor(professor);
            }

            em.persist(team);
            teams.add(team);
        }
        return teams;
    }

    // --- 리포트 -------------------------------------------------------------

    private record Result(List<String> legacyTitles, long legacyQueries, long legacyNanos,
                          List<String> improvedTitles, long improvedQueries, long improvedNanos) {

        String report(String title) {
            Function<Long, String> ms = nanos -> String.format("%.1fms", nanos / 1_000_000.0);
            List<String> lines = new ArrayList<>();
            lines.add("");
            lines.add("─────────────────────────────────────────────");
            lines.add(" " + title);
            lines.add("─────────────────────────────────────────────");
            lines.add(String.format("  개선 전  쿼리 %5d회   %s", legacyQueries, ms.apply(legacyNanos)));
            lines.add(String.format("  개선 후  쿼리 %5d회   %s", improvedQueries, ms.apply(improvedNanos)));
            lines.add(String.format("  감소     %.2f%%  (%d → %d, %.0f배)",
                    (1 - (double) improvedQueries / legacyQueries) * 100, legacyQueries, improvedQueries,
                    (double) legacyNanos / improvedNanos));
            lines.add("─────────────────────────────────────────────");
            return String.join(System.lineSeparator(), lines);
        }
    }
}
