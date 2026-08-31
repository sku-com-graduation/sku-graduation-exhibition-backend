package com.ghostHoliday.graduationExhibitions.performance;

import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 업로드 방식별로 백엔드가 실제로 무엇을 부담하는지 잰다.
 *
 * <p>비교 대상은 두 가지다.
 * <ul>
 *   <li><b>서버 중계</b> — 예전 구조. 프론트가 파일을 백엔드로 보내면 백엔드가 통째로 받아
 *       두었다가 다시 S3로 올린다. {@link BenchConfig.RelayUploadController}에 당시 코드를
 *       그대로 재현해 뒀다(git 0f92987 이전 S3Uploader.uploadToS3).</li>
 *   <li><b>Pre-signed URL</b> — 지금 구조. 백엔드는 업로드 URL만 발급하고 파일은 프론트가
 *       S3로 직접 보낸다.</li>
 * </ul>
 *
 * <p><b>이 테스트가 재는 것</b>은 백엔드가 지는 부담이다 — 통과한 바이트 수, 디스크에 만든
 * 임시 파일 크기, 요청 스레드를 붙잡은 시간. 세 가지 모두 네트워크 속도와 무관하게 구조에서
 * 나오는 값이라 로컬에서도 그대로 관측된다.
 *
 * <p><b>이 테스트가 재지 않는 것</b>은 사용자가 체감하는 업로드 완료 시간이다. 그건 실제
 * 네트워크와 S3가 있어야 의미가 있다 — {@code docs/perf/upload-benchmark.md} 참고.
 * 여기 찍히는 시간은 루프백 + 로컬 디스크 기준이라 "구간이 두 번 생긴다"는 사실만 보여준다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(UploadPathCostTest.BenchConfig.class)
class UploadPathCostTest {

    private static final Logger log = LoggerFactory.getLogger(UploadPathCostTest.class);

    private static final int MB = 1024 * 1024;
    private static final String BOUNDARY = "----UploadPathCostBoundary";

    /** 멀티파트 임시 파일이 떨어지는 위치. 여기 쌓이는 양을 감시해 백엔드 디스크 사용량을 잰다. */
    private static Path multipartTempDir;

    /** S3 대역. 실제 AWS 대신 로컬 HTTP 서버가 본문을 받아 버린다. */
    private static HttpServer s3Stub;
    private static String s3StubUrl;
    private static final AtomicLong s3StubBytesReceived = new AtomicLong();

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void multipartProperties(DynamicPropertyRegistry registry) throws IOException {
        multipartTempDir = Files.createTempDirectory("upload-bench-multipart");
        registry.add("spring.servlet.multipart.location", () -> multipartTempDir.toString());
        registry.add("spring.servlet.multipart.max-file-size", () -> "600MB");
        registry.add("spring.servlet.multipart.max-request-size", () -> "600MB");
        // 크기와 무관하게 항상 디스크로 떨어뜨려 관측을 단순하게 한다(Spring 기본값도 0이다).
        registry.add("spring.servlet.multipart.file-size-threshold", () -> "0");
    }

    @BeforeAll
    static void startS3Stub() throws IOException {
        s3Stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s3Stub.createContext("/", exchange -> {
            long read = drain(exchange.getRequestBody());
            s3StubBytesReceived.addAndGet(read);
            exchange.getResponseHeaders().add("ETag", "\"stub-etag\"");
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        s3Stub.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        s3Stub.start();
        s3StubUrl = "http://127.0.0.1:" + s3Stub.getAddress().getPort();
        BenchConfig.s3StubUrl = s3StubUrl;
    }

    @AfterAll
    static void stopS3Stub() throws IOException {
        if (s3Stub != null) {
            s3Stub.stop(0);
        }
        if (multipartTempDir != null) {
            try (Stream<Path> paths = Files.list(multipartTempDir)) {
                paths.forEach(p -> p.toFile().delete());
            }
            Files.deleteIfExists(multipartTempDir);
        }
    }

    @BeforeEach
    void resetCounters() {
        s3StubBytesReceived.set(0);
        BenchConfig.reset();
    }

    @Test
    @DisplayName("10MB 업로드 — 백엔드 부담 비교")
    void upload10MB() throws Exception {
        Result relay = measureRelay(10);
        Result presigned = measurePresigned(10);
        log.info(report("10MB 파일 1건", relay, presigned));
        assertBackendIsFreeOfFileBytes(relay, presigned, 10);
    }

    @Test
    @DisplayName("100MB 업로드 — 백엔드 부담 비교")
    void upload100MB() throws Exception {
        Result relay = measureRelay(100);
        Result presigned = measurePresigned(100);
        log.info(report("100MB 파일 1건", relay, presigned));
        assertBackendIsFreeOfFileBytes(relay, presigned, 100);
    }

    private void assertBackendIsFreeOfFileBytes(Result relay, Result presigned, int sizeMb) {
        long fileBytes = (long) sizeMb * MB;

        // 서버 중계: 받은 만큼 그대로 다시 보낸다 = 파일 크기의 두 배가 백엔드를 통과한다.
        assertThat(relay.backendBytes).isEqualTo(fileBytes * 2);
        // 그리고 그 파일이 통째로 백엔드 디스크에 임시 파일로 떨어진다.
        assertThat(relay.maxTempBytes).isGreaterThanOrEqualTo(fileBytes);

        // Pre-signed: 백엔드는 URL 한 줄만 만든다. 파일 바이트는 백엔드를 지나가지 않는다.
        assertThat(presigned.backendBytes).isLessThan(1024);
        assertThat(presigned.maxTempBytes).isZero();

        // 두 경로 모두 S3 에는 파일이 한 번씩 도착한다 — 결과는 같고 경로만 다르다.
        assertThat(presigned.s3Bytes).isEqualTo(fileBytes);
        assertThat(relay.s3Bytes).isEqualTo(fileBytes);
    }

    // --- 측정 ---------------------------------------------------------------

    /** 예전 구조: 프론트 → 백엔드(전체 수신·디스크 버퍼링) → S3. */
    private Result measureRelay(int sizeMb) throws Exception {
        long fileBytes = (long) sizeMb * MB;
        TempDirWatcher watcher = TempDirWatcher.start(multipartTempDir);

        long start = System.nanoTime();
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/public/bench/relay"))
                        .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                        .POST(HttpRequest.BodyPublishers.ofInputStream(() -> multipartBody(fileBytes)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        long elapsedNanos = System.nanoTime() - start;

        long maxTemp = watcher.stopAndGetMax();
        assertThat(response.statusCode()).isEqualTo(200);

        return new Result(
                elapsedNanos,
                BenchConfig.requestThreadNanos.get(),
                BenchConfig.bytesReceived.get() + BenchConfig.bytesForwarded.get(),
                maxTemp,
                s3StubBytesReceived.get(),
                BenchConfig.s3ForwardNanos.get());
    }

    /** 지금 구조: 백엔드는 URL만 발급 → 프론트 → S3 직접. */
    private Result measurePresigned(int sizeMb) throws Exception {
        long fileBytes = (long) sizeMb * MB;
        s3StubBytesReceived.set(0);
        BenchConfig.reset();
        TempDirWatcher watcher = TempDirWatcher.start(multipartTempDir);

        long start = System.nanoTime();

        HttpResponse<String> urlResponse = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/public/bench/presigned-url"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(urlResponse.statusCode()).isEqualTo(200);
        String uploadUrl = urlResponse.body().replaceAll(".*\"url\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        HttpResponse<Void> putResponse = client.send(
                HttpRequest.newBuilder(URI.create(uploadUrl))
                        .header("Content-Type", "video/mp4")
                        .PUT(HttpRequest.BodyPublishers.ofInputStream(() -> new ZeroInputStream(fileBytes)))
                        .build(),
                HttpResponse.BodyHandlers.discarding());
        assertThat(putResponse.statusCode()).isEqualTo(200);

        long elapsedNanos = System.nanoTime() - start;
        long maxTemp = watcher.stopAndGetMax();

        return new Result(
                elapsedNanos,
                BenchConfig.requestThreadNanos.get(),
                urlResponse.body().getBytes(StandardCharsets.UTF_8).length,
                maxTemp,
                s3StubBytesReceived.get(),
                0);
    }

    // --- 리포트 -------------------------------------------------------------

    private static String report(String title, Result relay, Result presigned) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add("──────────────────────────────────────────────────────────────");
        lines.add(" " + title + " — 백엔드가 지는 부담");
        lines.add("──────────────────────────────────────────────────────────────");
        lines.add(String.format("  %-26s %14s %14s", "", "서버 중계(이전)", "Pre-signed(현재)"));
        lines.add(String.format("  %-26s %14s %14s", "백엔드 통과 바이트",
                mb(relay.backendBytes), mb(presigned.backendBytes)));
        lines.add(String.format("  %-26s %14s %14s", "백엔드 임시 파일 최대",
                mb(relay.maxTempBytes), mb(presigned.maxTempBytes)));
        lines.add(String.format("  %-26s %14s %14s", "요청 스레드 점유 시간",
                ms(relay.requestThreadNanos), ms(presigned.requestThreadNanos)));
        lines.add(String.format("  %-26s %14s %14s", "S3 로 다시 보낸 시간",
                ms(relay.s3ForwardNanos), "없음"));
        lines.add(String.format("  %-26s %14s %14s", "S3 도착 바이트",
                mb(relay.s3Bytes), mb(presigned.s3Bytes)));
        lines.add("──────────────────────────────────────────────────────────────");
        lines.add("  ※ 위 세 줄은 네트워크 속도와 무관한 구조적 비용이다.");
        lines.add("  ※ 사용자 체감 업로드 시간은 실제 S3 로 재야 한다(docs/perf 참고).");
        lines.add("──────────────────────────────────────────────────────────────");
        return String.join(System.lineSeparator(), lines);
    }

    private static String mb(long bytes) {
        if (bytes == 0) return "0";
        if (bytes < MB) return bytes + " B";
        return String.format("%.0f MB", bytes / (double) MB);
    }

    private static String ms(long nanos) {
        return String.format("%.0f ms", nanos / 1_000_000.0);
    }

    private record Result(long elapsedNanos, long requestThreadNanos,
                          long backendBytes, long maxTempBytes, long s3Bytes,
                          long s3ForwardNanos) {
    }

    // --- 보조 도구 -----------------------------------------------------------

    /** 멀티파트 임시 디렉터리에 쌓이는 바이트를 표본 추출해 최대치를 남긴다. */
    private static final class TempDirWatcher {
        private final AtomicLong max = new AtomicLong();
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final Thread thread;

        private TempDirWatcher(Path dir) {
            this.thread = new Thread(() -> {
                while (running.get()) {
                    max.accumulateAndGet(directorySize(dir), Math::max);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
            this.thread.setDaemon(true);
        }

        static TempDirWatcher start(Path dir) {
            TempDirWatcher watcher = new TempDirWatcher(dir);
            watcher.thread.start();
            return watcher;
        }

        long stopAndGetMax() throws InterruptedException {
            running.set(false);
            thread.join(1000);
            return max.get();
        }

        private static long directorySize(Path dir) {
            try (Stream<Path> paths = Files.list(dir)) {
                return paths.mapToLong(p -> p.toFile().length()).sum();
            } catch (IOException e) {
                return 0;
            }
        }
    }

    /** 지정한 크기만큼 0 을 흘려보내는 스트림. 큰 파일을 메모리에 올리지 않기 위해 쓴다. */
    private static final class ZeroInputStream extends InputStream {
        private long remaining;

        ZeroInputStream(long size) {
            this.remaining = size;
        }

        @Override
        public int read() {
            if (remaining <= 0) return -1;
            remaining--;
            return 0;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (remaining <= 0) return -1;
            int n = (int) Math.min(len, remaining);
            java.util.Arrays.fill(b, off, off + n, (byte) 0);
            remaining -= n;
            return n;
        }
    }

    private static InputStream multipartBody(long fileBytes) {
        String head = "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"demo.mp4\"\r\n"
                + "Content-Type: video/mp4\r\n\r\n";
        String tail = "\r\n--" + BOUNDARY + "--\r\n";

        return new SequenceInputStream(Collections.enumeration(List.of(
                new java.io.ByteArrayInputStream(head.getBytes(StandardCharsets.UTF_8)),
                new ZeroInputStream(fileBytes),
                new java.io.ByteArrayInputStream(tail.getBytes(StandardCharsets.UTF_8)))));
    }

    private static long drain(InputStream in) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
        }
        return total;
    }

    // --- 벤치 전용 엔드포인트 -------------------------------------------------

    @TestConfiguration
    static class BenchConfig {

        static volatile String s3StubUrl;
        static final AtomicLong requestThreadNanos = new AtomicLong();
        static final AtomicLong bytesReceived = new AtomicLong();
        static final AtomicLong bytesForwarded = new AtomicLong();
        /** 파일을 다 받은 뒤 S3 로 다시 보내는 데만 쓴 시간. 중계 구조가 업로드에 얹는 구간이다. */
        static final AtomicLong s3ForwardNanos = new AtomicLong();

        static void reset() {
            requestThreadNanos.set(0);
            bytesReceived.set(0);
            bytesForwarded.set(0);
            s3ForwardNanos.set(0);
        }

        /** 요청 스레드가 실제로 묶여 있던 시간 — 멀티파트 수신까지 포함해야 하므로 필터에서 잰다. */
        @Bean
        Filter benchTimingFilter() {
            return (ServletRequest request, ServletResponse response, FilterChain chain) -> {
                long start = System.nanoTime();
                try {
                    chain.doFilter(request, response);
                } finally {
                    requestThreadNanos.set(System.nanoTime() - start);
                }
            };
        }

        // 아래 두 컨트롤러는 이 설정 클래스의 중첩 @RestController 라 자동 등록된다.

        /**
         * 예전 구조 재현 — git 0f92987 이전의 S3Uploader.uploadToS3 와 같은 모양이다.
         * 컨트롤러가 호출되는 시점에는 이미 서블릿 컨테이너가 파일 전체를 받아 디스크에
         * 떨궈 둔 상태다. 그 뒤에야 S3 전송이 시작되므로 두 구간은 겹치지 않고 차례로 흐른다.
         */
        @RestController
        static class RelayUploadController {

            @PostMapping("/api/public/bench/relay")
            public Map<String, Object> relay(@RequestParam("file") MultipartFile file) throws Exception {
                bytesReceived.set(file.getSize());

                // 여기 도달한 시점에 파일은 이미 다 도착해 디스크에 있다.
                // 아래 구간이 사용자 전송이 끝난 뒤에 따로 흐르는 되보내기다.
                long forwardStart = System.nanoTime();
                HttpClient client = HttpClient.newHttpClient();
                try (InputStream in = file.getInputStream()) {
                    HttpResponse<Void> response = client.send(
                            HttpRequest.newBuilder(URI.create(s3StubUrl + "/uploads/demo.mp4"))
                                    .header("Content-Type", file.getContentType())
                                    .PUT(HttpRequest.BodyPublishers.ofInputStream(() -> in))
                                    .build(),
                            HttpResponse.BodyHandlers.discarding());
                    if (response.statusCode() != 200) {
                        throw new ServletException("S3 stub 응답 이상: " + response.statusCode());
                    }
                }
                s3ForwardNanos.set(System.nanoTime() - forwardStart);
                bytesForwarded.set(file.getSize());

                return Map.of("size", file.getSize());
            }
        }

        /** 지금 구조 — 백엔드는 업로드 URL 한 줄만 만들어 준다. */
        @RestController
        static class PresignedUrlController {

            @GetMapping("/api/public/bench/presigned-url")
            public Map<String, String> issue() {
                return Map.of("url", s3StubUrl + "/uploads/demo.mp4");
            }
        }
    }
}
