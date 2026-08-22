package com.ghostHoliday.graduationExhibitions.security;

import com.ghostHoliday.graduationExhibitions.domain.Role;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자 전용 API 의 인가가 실제로 걸리는지 실제 HTTP 로 확인한다.
 *
 * <p>이 테스트를 쓴 이유가 있다. 컨트롤러 19곳에 {@code @PreAuthorize} 가 붙어 있었지만
 * {@code @EnableMethodSecurity} 가 없어 <b>전부 무시되고 있었다</b>. 실제 방어는 SecurityConfig 의
 * URL 규칙 하나뿐이었고, 경로 이름이 우연히 {@code /api/admin/**} 로 맞아떨어져 문제가 드러나지
 * 않았을 뿐이다. 그 규칙 밖에 엔드포인트를 하나 추가하는 순간 아무 보호 없이 열렸을 것이다.
 *
 * <p>게다가 표현식은 {@code hasAuthority('ADMIN')} 인데 로그인 시 부여되는 권한은
 * {@code ROLE_ADMIN} 이었다. 그래서 메서드 보안을 켜기만 했다면 <b>관리자 전원이 403</b> 이 됐다.
 * 활성화와 표현식 정정을 함께 해야 했고, 그게 맞았는지 여기서 확인한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminAuthorizationTest {

    /** 관리자 전용 조회 — AccountController 의 @PreAuthorize 와 URL 규칙이 둘 다 걸리는 경로. */
    private static final String ADMIN_ENDPOINT = "/api/admin/account/search?year=2025";

    @LocalServerPort
    private int port;

    @Autowired
    private JwtUtility jwtUtility;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    @DisplayName("관리자 토큰이면 통과한다 — 표현식 정정이 맞았는지 확인")
    void adminPasses() throws Exception {
        int status = call(jwtUtility.generateToken("admin@test.local", Role.ADMIN));

        assertThat(status)
                .as("관리자가 403 이면 @PreAuthorize 표현식이 실제 권한 이름과 어긋난 것이다")
                .isNotEqualTo(403);
        assertThat(status).isIn(200, 204);
    }

    @Test
    @DisplayName("일반 사용자 토큰이면 막힌다 — 인가가 실제로 걸린다는 증거")
    void userIsForbidden() throws Exception {
        assertThat(call(jwtUtility.generateToken("user@test.local", Role.USER))).isEqualTo(403);
    }

    @Test
    @DisplayName("토큰이 없으면 막힌다")
    void anonymousIsRejected() throws Exception {
        assertThat(call(null)).isIn(401, 403);
    }

    @Test
    @DisplayName("공개 경로는 토큰 없이도 열려 있다")
    void publicEndpointStaysOpen() throws Exception {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(base() + "/api/public/home/search/info")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isNotIn(401, 403);
    }

    private int call(String accessToken) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base() + ADMIN_ENDPOINT)).GET();
        if (accessToken != null) {
            // 이 서비스는 토큰을 accessToken 쿠키로 받는다
            request.header("Cookie", "accessToken=" + accessToken);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    private String base() {
        return "http://localhost:" + port;
    }
}
