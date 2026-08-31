package com.ghostHoliday.graduationExhibitions.performance;

import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 업로드가 몰릴 때 <b>다른 API 가 얼마나 느려지는지</b>를 잰다.
 *
 * <p>{@link UploadPathCostTest} 가 업로드 1건에 백엔드가 지는 비용(통과 바이트, 임시 파일,
 * 스레드 점유 시간)을 잰다면, 여기서는 그 비용이 쌓였을 때 사용자가 겪는 시간을 잰다.
 * 업로드하는 사람이 아니라 <b>그 옆에서 목록을 보고 있는 사람</b>의 응답 시간이다.
 *
 * <p><b>왜 이걸 재나.</b> 서버 중계 구조에서 {@code MultipartFile} 을 받는다는 건 파일이 다
 * 도착할 때까지 톰캣 요청 스레드 하나가 묶여 있다는 뜻이다. 사용자의 회선이 느릴수록 오래
 * 묶인다. 동시 업로드가 스레드 수를 넘기면 그 뒤에 온 평범한 조회 요청까지 큐에서 기다린다.
 * Pre-signed URL 구조에서는 백엔드가 URL 한 줄만 만들고 빠지므로 이 일이 생기지 않는다.
 *
 * <p><b>측정 조건.</b> 스레드 풀을 {@value #TOMCAT_THREADS} 개로 줄이고 동시 업로드
 * {@value #CONCURRENT_UPLOADS} 건을 건다. 운영 기본값은 200 이므로, 같은 일이 벌어지는
 * 지점이 동시 업로드 200 건으로 옮겨갈 뿐 구조는 같다. 업로드 클라이언트는 상행
 * {@value #CLIENT_KB_PER_SEC} KB/s 로 흘려보낸다. 실제 사용자의 회선을 모사한 값이고,
 * 이 속도에서 {@value #FILE_MB}MB 는 약 {@code FILE_MB * 1024 / CLIENT_KB_PER_SEC} 초가 걸린다.
 *
 * <pre>
 * ./gradlew test --tests "*UploadConcurrencyCostTest"
 * </pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(UploadConcurrencyCostTest.BenchConfig.class)
class UploadConcurrencyCostTest {

    private static final Logger log = LoggerFactory.getLogger(UploadConcurrencyCostTest.class);

    private static final int MB = 1024 * 1024;
    private static final String BOUNDARY = "----UploadConcurrencyBoundary";

    /** 톰캣 요청 스레드 수. 운영 기본값 200 을 줄여 포화 지점을 앞당긴다. */
    private static final int TOMCAT_THREADS = 10;
    /** 동시에 올리는 사람 수. 스레드 수보다 많아야 큐가 생긴다. */
    private static final int CONCURRENT_UPLOADS = 16;
    private static final int FILE_MB = 8;
    /** 업로드하는 쪽의 상행 속도. 1024 KB/s = 약 8 Mbps. */
    private static final int CLIENT_KB_PER_SEC = 1024;

    // 부하 구간에서는 표본 하나가 업로드 한 건만큼 걸린다. 20 이면 분포를 보기에 충분하고
    // 테스트가 3분 안에 끝난다.
    private static final int PING_SAMPLES = 20;
    private static final long PING_INTERVAL_MS = 150;

    private static Path multipartTempDir;
    private static HttpServer s3Stub;
    private static String s3StubUrl;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws IOException {
        multipartTempDir = Files.createTempDirectory("upload-concurrency-multipart");
        registry.add("spring.servlet.multipart.location", () -> multipartTempDir.toString());
        registry.add("spring.servlet.multipart.max-file-size", () -> "600MB");
        registry.add("spring.servlet.multipart.max-request-size", () -> "600MB");
        registry.add("spring.servlet.multipart.file-size-threshold", () -> "0");
        registry.add("server.tomcat.threads.max", () -> String.valueOf(TOMCAT_THREADS));
        registry.add("server.tomcat.threads.min-spare", () -> String.valueOf(TOMCAT_THREADS));
    }

    @BeforeAll
    static void startS3Stub() throws IOException {
        s3Stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s3Stub.createContext("/", exchange -> {
            drain(exchange.getRequestBody());
            exchange.getResponseHeaders().add("ETag", "\"stub-etag\"");
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        // S3 는 요청 수에 맞춰 늘어난다. 스텁도 요청마다 스레드를 준다.
        s3Stub.setExecutor(Executors.newCachedThreadPool());
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

    @Test
    @DisplayName("동시 업로드가 몰릴 때 조회 API 응답 시간")
    void listApiLatencyUnderUploadBurst() throws Exception {
        long[] idle = measurePing(null);

        long[] duringRelay = measurePing(this::startRelayUploads);
        long[] duringPresigned = measurePing(this::startPresignedUploads);

        log.info(report(idle, duringRelay, duringPresigned));

        // 서버 중계에서는 업로드가 조회를 밀어낸다. 개선 후에는 그 영향이 남지 않는다.
        assertThat(percentile(duringRelay, 50)).isGreaterThan(percentile(idle, 50) * 10);
        assertThat(percentile(duringPresigned, 95)).isLessThan(percentile(duringRelay, 50));
    }

    @Test
    @DisplayName("업로드 1건이 끝나기까지 걸린 시간")
    void singleUploadWallClock() throws Exception {
        long relayNanos = timeRelayUpload();
        long presignedNanos = timePresignedUpload();

        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(line());
        lines.add(String.format(" 업로드 1건 완료까지 걸린 시간 (%dMB, 상행 %d KB/s)", FILE_MB, CLIENT_KB_PER_SEC));
        lines.add(line());
        lines.add(String.format("  서버 중계(이전)      %s", ms(relayNanos)));
        lines.add(String.format("  Pre-signed(현재)     %s", ms(presignedNanos)));
        lines.add(String.format("  차이                 %s", ms(relayNanos - presignedNanos)));
        lines.add(line());
        lines.add("  두 경로 모두 사용자의 상행 회선이 병목이라 총 시간은 크게 다르지 않다.");
        lines.add("  줄어든 만큼이 백엔드가 S3 로 다시 올리던 구간이다. 여기서는 루프백이라 짧게");
        lines.add("  나오고, 실제 EC2 에서 S3 로 보내는 구간은 이보다 길다.");
        lines.add(line());
        log.info(String.join(System.lineSeparator(), lines));

        // 중계 경로는 받은 뒤에 다시 보내므로 두 구간이 겹치지 않고 차례로 흐른다.
        assertThat(relayNanos).isGreaterThan(presignedNanos);
    }

    // --- 측정 ---------------------------------------------------------------

    /**
     * 조회 API 응답 시간을 표본 추출한다. {@code burst} 가 주어지면 그 부하를 걸어 둔 채로 잰다.
     */
    private long[] measurePing(BurstStarter burst) throws Exception {
        Burst running = burst == null ? null : burst.start();
        try {
            if (running != null) {
                // 업로드들이 실제로 스레드를 잡을 때까지 기다린다.
                Thread.sleep(700);
            }
            HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
            long[] samples = new long[PING_SAMPLES];
            for (int i = 0; i < PING_SAMPLES; i++) {
                long start = System.nanoTime();
                HttpResponse<String> response = client.send(
                        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/public/bench/ping"))
                                .GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                samples[i] = System.nanoTime() - start;
                assertThat(response.statusCode()).isEqualTo(200);
                Thread.sleep(PING_INTERVAL_MS);
            }
            return samples;
        } finally {
            if (running != null) {
                running.stop();
            }
        }
    }

    private Burst startRelayUploads() {
        return startBurst(() -> {
            HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
            client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/public/bench/relay"))
                            .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                            .POST(HttpRequest.BodyPublishers.ofInputStream(
                                    () -> throttledMultipartBody((long) FILE_MB * MB)))
                            .build(),
                    HttpResponse.BodyHandlers.discarding());
        });
    }

    private Burst startPresignedUploads() {
        return startBurst(() -> {
            HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
            HttpResponse<String> urlResponse = client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/public/bench/presigned-url"))
                            .GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            String uploadUrl = urlResponse.body().replaceAll(".*\"url\"\\s*:\\s*\"([^\"]+)\".*", "$1");
            client.send(
                    HttpRequest.newBuilder(URI.create(uploadUrl))
                            .header("Content-Type", "video/mp4")
                            .PUT(HttpRequest.BodyPublishers.ofInputStream(
                                    () -> new ThrottledZeroStream((long) FILE_MB * MB, CLIENT_KB_PER_SEC)))
                            .build(),
                    HttpResponse.BodyHandlers.discarding());
        });
    }

    private Burst startBurst(UploadTask task) {
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_UPLOADS);
        AtomicBoolean keepGoing = new AtomicBoolean(true);
        CountDownLatch started = new CountDownLatch(CONCURRENT_UPLOADS);

        for (int i = 0; i < CONCURRENT_UPLOADS; i++) {
            pool.submit(() -> {
                started.countDown();
                while (keepGoing.get()) {
                    try {
                        task.run();
                    } catch (Exception e) {
                        if (keepGoing.get()) {
                            log.debug("업로드 실패", e);
                        }
                        return;
                    }
                }
            });
        }
        try {
            started.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return () -> {
            keepGoing.set(false);
            pool.shutdownNow();
            pool.awaitTermination(30, TimeUnit.SECONDS);
        };
    }

    private long timeRelayUpload() throws Exception {
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        long start = System.nanoTime();
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/public/bench/relay"))
                        .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                        .POST(HttpRequest.BodyPublishers.ofInputStream(
                                () -> throttledMultipartBody((long) FILE_MB * MB)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        long elapsed = System.nanoTime() - start;
        assertThat(response.statusCode()).isEqualTo(200);
        return elapsed;
    }

    private long timePresignedUpload() throws Exception {
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        long start = System.nanoTime();
        HttpResponse<String> urlResponse = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/public/bench/presigned-url"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
        String uploadUrl = urlResponse.body().replaceAll(".*\"url\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        HttpResponse<Void> putResponse = client.send(
                HttpRequest.newBuilder(URI.create(uploadUrl))
                        .header("Content-Type", "video/mp4")
                        .PUT(HttpRequest.BodyPublishers.ofInputStream(
                                () -> new ThrottledZeroStream((long) FILE_MB * MB, CLIENT_KB_PER_SEC)))
                        .build(),
                HttpResponse.BodyHandlers.discarding());
        long elapsed = System.nanoTime() - start;
        assertThat(putResponse.statusCode()).isEqualTo(200);
        return elapsed;
    }

    // --- 리포트 -------------------------------------------------------------

    private static String report(long[] idle, long[] relay, long[] presigned) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(line());
        lines.add(String.format(" 업로드가 몰릴 때 조회 API 응답 시간 (동시 업로드 %d건, 스레드 %d개)",
                CONCURRENT_UPLOADS, TOMCAT_THREADS));
        lines.add(line());
        lines.add(String.format("  %-24s %12s %12s %12s", "", "p50", "p95", "최대"));
        lines.add(String.format("  %-24s %12s %12s %12s", "업로드 없음",
                ms(percentile(idle, 50)), ms(percentile(idle, 95)), ms(max(idle))));
        lines.add(String.format("  %-24s %12s %12s %12s", "서버 중계(이전)",
                ms(percentile(relay, 50)), ms(percentile(relay, 95)), ms(max(relay))));
        lines.add(String.format("  %-24s %12s %12s %12s", "Pre-signed(현재)",
                ms(percentile(presigned, 50)), ms(percentile(presigned, 95)), ms(max(presigned))));
        lines.add(line());
        lines.add(String.format("  파일 %dMB, 업로드 쪽 상행 %d KB/s, 표본 %d회.",
                FILE_MB, CLIENT_KB_PER_SEC, PING_SAMPLES));
        lines.add("  조회 API 는 DB 를 타지 않는 가장 가벼운 요청이다. 그래도 서버 중계에서는");
        lines.add("  업로드가 스레드를 다 잡고 있어 큐에서 기다린다.");
        lines.add(line());
        return String.join(System.lineSeparator(), lines);
    }

    private static String line() {
        return "──────────────────────────────────────────────────────────────";
    }

    private static String ms(long nanos) {
        return String.format("%.1f ms", nanos / 1_000_000.0);
    }

    private static long percentile(long[] values, int p) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        int index = (int) Math.ceil(p / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(index, sorted.length - 1))];
    }

    private static long max(long[] values) {
        return Arrays.stream(values).max().orElse(0);
    }

    // --- 보조 도구 -----------------------------------------------------------

    @FunctionalInterface
    private interface UploadTask {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface BurstStarter {
        Burst start();
    }

    @FunctionalInterface
    private interface Burst {
        void stop() throws InterruptedException;
    }

    /** 지정한 속도로만 0 을 흘려보내는 스트림. 느린 회선을 모사한다. */
    private static final class ThrottledZeroStream extends InputStream {
        private final long kbPerSec;
        private final long start = System.nanoTime();
        private long remaining;
        private long written;

        ThrottledZeroStream(long size, long kbPerSec) {
            this.remaining = size;
            this.kbPerSec = kbPerSec;
        }

        @Override
        public int read() {
            byte[] one = new byte[1];
            return read(one, 0, 1) == -1 ? -1 : 0;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (remaining <= 0) return -1;
            int n = (int) Math.min(Math.min(len, remaining), 16 * 1024);
            Arrays.fill(b, off, off + n, (byte) 0);
            remaining -= n;
            written += n;
            throttle();
            return n;
        }

        /** 지금까지 보낸 양이 허용치를 넘었으면 그만큼 쉰다. */
        private void throttle() {
            long allowedNanos = written * 1_000_000_000L / (kbPerSec * 1024L);
            long elapsed = System.nanoTime() - start;
            long sleepNanos = allowedNanos - elapsed;
            if (sleepNanos > 0) {
                try {
                    Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static InputStream throttledMultipartBody(long fileBytes) {
        String head = "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"demo.mp4\"\r\n"
                + "Content-Type: video/mp4\r\n\r\n";
        String tail = "\r\n--" + BOUNDARY + "--\r\n";

        return new SequenceInputStream(Collections.enumeration(List.of(
                new ByteArrayInputStream(head.getBytes(StandardCharsets.UTF_8)),
                new ThrottledZeroStream(fileBytes, CLIENT_KB_PER_SEC),
                new ByteArrayInputStream(tail.getBytes(StandardCharsets.UTF_8)))));
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

        /** 예전 구조. 파일을 통째로 받아 둔 뒤에야 컨트롤러가 호출되고, 그 뒤에 S3 로 다시 보낸다. */
        @RestController
        static class RelayUploadController {

            @PostMapping("/api/public/bench/relay")
            public Map<String, Object> relay(@RequestParam("file") MultipartFile file) throws Exception {
                HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
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
                return Map.of("size", file.getSize());
            }
        }

        /** 지금 구조. 백엔드는 업로드 URL 한 줄만 만들어 준다. */
        @RestController
        static class PresignedUrlController {

            @GetMapping("/api/public/bench/presigned-url")
            public Map<String, String> issue() {
                return Map.of("url", s3StubUrl + "/uploads/demo.mp4");
            }
        }

        /** 업로드와 무관한 가장 가벼운 조회 요청. 이게 느려지면 옆 사람이 느려진 것이다. */
        @RestController
        static class PingController {

            @GetMapping("/api/public/bench/ping")
            public String ping() {
                return "pong";
            }
        }
    }
}
