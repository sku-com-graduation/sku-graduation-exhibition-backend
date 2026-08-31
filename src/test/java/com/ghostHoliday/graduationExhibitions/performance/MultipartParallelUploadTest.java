package com.ghostHoliday.graduationExhibitions.performance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S3 멀티파트 병렬 업로드가 파일 크기별로 얼마나 이득인지 잰다.
 *
 * <p><b>왜 따로 재나.</b> Pre-signed URL 은 파일이 백엔드를 지나가지 않게 만든 개선이고,
 * 멀티파트 병렬은 그 위에 올린 별개의 개선이다. 앞의 것은
 * {@link UploadPathCostTest} 와 {@link UploadConcurrencyCostTest} 가 뒷받침하지만 뒤의 것은
 * 숫자가 없었다.
 *
 * <p><b>루프백에서는 잴 수 없다.</b> 커넥션 하나의 처리량은 대략 혼잡 윈도우 나누기 RTT 다.
 * 루프백은 RTT 가 0 에 가까워 커넥션 하나로도 대역을 다 쓰고, 그래서 병렬로 나눠도 시간이
 * 똑같이 나온다. 그 상태로 재면 "멀티파트는 효과 없음" 이라는 잘못된 결론이 나온다.
 * 병렬이 이기는 건 RTT 가 있어서 커넥션 하나가 윈도우 제한에 걸릴 때다.
 *
 * <p><b>그래서 지연이 있는 링크를 만든다.</b> S3 자리에 도커 컨테이너를 놓고 그 안에서
 * {@code tc netem} 으로 실제 지연을 건다. 커널이 진짜로 패킷을 늦추므로 TCP 혼잡 제어와
 * 윈도우 증가가 실제로 일어난다. 모델을 계산한 게 아니라 실제 TCP 동작을 잰 것이다.
 * 절대값은 AWS 와 다르지만, 병렬이 이기는 이유와 그 크기가 RTT 에 어떻게 걸리는지는 같다.
 *
 * <pre>
 * # 지연 링크 준비 (docs/perf/upload-benchmark.md 참고)
 * docker run -d --name s3sink --cap-add=NET_ADMIN -p 9000:9000 \
 *   -v "$PWD/src/test/resources/bench:/app" -w /app python:3.12-slim python sink.py
 * docker exec s3sink sh -c "apt-get update -qq && apt-get install -y -qq iproute2"
 * docker exec s3sink tc qdisc add dev eth0 root netem delay 30ms
 *
 * ./gradlew test --tests "*MultipartParallelUploadTest" \
 *   -Dbench.sink=http://localhost:9000 -Dbench.rtt=30ms
 * </pre>
 */
@EnabledIfSystemProperty(named = "bench.sink", matches = "https?://.+")
class MultipartParallelUploadTest {

    private static final Logger log = LoggerFactory.getLogger(MultipartParallelUploadTest.class);

    private static final int MB = 1024 * 1024;
    /** S3 멀티파트의 최소 파트 크기는 5MB 다. 앱도 그 위에서 고른다. */
    private static final int PART_MB = 10;
    private static final int ROUNDS = 3;

    // 실제로 올라오는 건 영상이다. 이미지 크기대로 재면 멀티파트를 쓸 이유가 안 보인다.
    private static final int[] SIZES_MB = {100, 500, 1024};
    private static final int[] CONCURRENCY = {2, 4, 8};

    private final String sink = System.getProperty("bench.sink");
    private final String rttLabel = System.getProperty("bench.rtt", "미지정");

    @Test
    @DisplayName("파일 크기별 단일 PUT 대 멀티파트 병렬")
    void singlePutVersusParallelMultipart() throws Exception {
        List<Row> rows = new ArrayList<>();

        for (int sizeMb : SIZES_MB) {
            long[] single = new long[ROUNDS];
            long[][] parallel = new long[CONCURRENCY.length][ROUNDS];

            for (int r = 0; r < ROUNDS; r++) {
                single[r] = timeSinglePut(sizeMb, r);
                for (int c = 0; c < CONCURRENCY.length; c++) {
                    parallel[c][r] = timeParallelMultipart(sizeMb, CONCURRENCY[c], r);
                }
            }

            long[] medians = new long[CONCURRENCY.length];
            for (int c = 0; c < CONCURRENCY.length; c++) {
                medians[c] = median(parallel[c]);
            }
            rows.add(new Row(sizeMb, median(single), medians));
        }

        log.info(report(rows));

        // 지연이 걸린 링크에서는 병렬이 단일 커넥션보다 빨라야 한다.
        Row biggest = rows.get(rows.size() - 1);
        assertThat(biggest.parallelNanos[CONCURRENCY.length - 1]).isLessThan(biggest.singleNanos);
    }

    @Test
    @DisplayName("업로드가 끊겼을 때 다시 보내야 하는 양")
    void retransmissionCostOnFailure() throws Exception {
        long total = 1000L * MB;
        long part = (long) PART_MB * MB;
        int parts = (int) (total / part);

        // 두 경로 모두 같은 지점에서 끊는다. 마지막 파트를 절반쯤 보낸 시점이다.
        long cutInLastPart = part / 2;
        long cutForSingle = total - part + cutInLastPart;

        // 단일 PUT: 끊기면 처음부터 다시 보낸다.
        AtomicLong singleSent = new AtomicLong();
        cut(cutForSingle, "single-fail", total, singleSent);
        AtomicLong singleResent = new AtomicLong();
        send("single-retry", total, singleResent);

        // 멀티파트: 앞 파트들은 이미 S3 에 올라가 있다. 끊긴 파트만 다시 보낸다.
        AtomicLong multipartSent = new AtomicLong();
        for (int i = 0; i < parts - 1; i++) {
            send("mp-" + i, part, multipartSent);
        }
        cut(cutInLastPart, "mp-fail", part, multipartSent);
        AtomicLong multipartResent = new AtomicLong();
        send("mp-retry", part, multipartResent);

        log.info(retransmissionReport(total, part,
                singleSent.get(), singleResent.get(),
                multipartSent.get(), multipartResent.get()));

        // 단일 PUT 은 파일 전체를, 멀티파트는 파트 하나만 다시 보낸다.
        assertThat(singleResent.get()).isEqualTo(total);
        assertThat(multipartResent.get()).isEqualTo(part);
    }

    /** 서버가 도중에 끊는 요청. 끊길 때까지 실제로 흘려보낸 바이트를 센다. */
    private void cut(long cutAfter, String key, long bytes, AtomicLong counter) throws Exception {
        try {
            send("cut/" + cutAfter + "/" + key, bytes, counter);
            throw new AssertionError("끊겼어야 하는데 성공했다: " + key);
        } catch (IOException expected) {
            // 서버가 연결을 닫아 생기는 예외다. 여기까지 보낸 양은 counter 에 남는다.
        }
    }

    private void send(String key, long bytes, AtomicLong counter) throws Exception {
        HttpClient client = newClient();
        client.send(
                HttpRequest.newBuilder(URI.create(sink + "/" + key))
                        .header("Content-Type", "application/octet-stream")
                        .PUT(HttpRequest.BodyPublishers.fromPublisher(
                                HttpRequest.BodyPublishers.ofInputStream(
                                        () -> new ZeroInputStream(bytes, counter)),
                                bytes))
                        .build(),
                HttpResponse.BodyHandlers.discarding());
    }

    private String retransmissionReport(long total, long part,
                                        long singleSent, long singleResent,
                                        long multipartSent, long multipartResent) {
        String bar = "──────────────────────────────────────────────────────────────────────";
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(bar);
        lines.add(String.format(" 업로드가 끊겼을 때 다시 보내야 하는 양 (%dMB 파일, 파트 %dMB)",
                total / MB, part / MB));
        lines.add(bar);
        lines.add(String.format("  %-22s %14s %14s", "", "단일 PUT", "멀티파트"));
        lines.add(String.format("  %-22s %14s %14s", "끊기기 전까지 보낸 양",
                mb(singleSent), mb(multipartSent)));
        lines.add(String.format("  %-22s %14s %14s", "다시 보낸 양",
                mb(singleResent), mb(multipartResent)));
        lines.add(String.format("  %-22s %14s %14s", "합계",
                mb(singleSent + singleResent), mb(multipartSent + multipartResent)));
        lines.add(bar);
        lines.add("  파일의 99% 지점, 마지막 파트를 절반쯤 보낸 시점에서 서버가 연결을 끊는다.");
        lines.add("  단일 PUT 은 어디서 끊겨도 처음부터다. 멀티파트는 끝난 파트가 S3 에 남아 있어");
        lines.add("  끊긴 파트만 다시 보낸다. 시간이 아니라 바이트라 링크 지연과 무관하다.");
        lines.add(bar);
        return String.join(System.lineSeparator(), lines);
    }

    private static String mb(long bytes) {
        return String.format("%,d MB", bytes / MB);
    }

    // --- 측정 ---------------------------------------------------------------

    /** 파일 하나를 커넥션 하나로 통째로 올린다. 멀티파트를 쓰지 않던 경로. */
    private long timeSinglePut(int sizeMb, int round) throws Exception {
        HttpClient client = newClient();
        long start = System.nanoTime();
        HttpResponse<Void> response = put(client, "single-" + sizeMb + "-" + round, (long) sizeMb * MB);
        long elapsed = System.nanoTime() - start;
        assertThat(response.statusCode()).isEqualTo(200);
        return elapsed;
    }

    /**
     * 파일을 {@value #PART_MB}MB 파트로 잘라 동시에 {@code concurrency} 개씩 올린다.
     * 파트마다 클라이언트를 따로 만들어 커넥션이 실제로 나뉘게 한다.
     */
    private long timeParallelMultipart(int sizeMb, int concurrency, int round) throws Exception {
        long total = (long) sizeMb * MB;
        long partSize = (long) PART_MB * MB;
        int parts = (int) Math.ceil((double) total / partSize);

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < parts; i++) {
            long bytes = Math.min(partSize, total - i * partSize);
            String key = "part-" + sizeMb + "-" + concurrency + "-" + round + "-" + i;
            tasks.add(() -> put(newClient(), key, bytes).statusCode());
        }

        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        long start;
        try {
            start = System.nanoTime();
            List<Future<Integer>> futures = pool.invokeAll(tasks);
            for (Future<Integer> f : futures) {
                assertThat(f.get()).isEqualTo(200);
            }
        } finally {
            pool.shutdown();
            pool.awaitTermination(60, TimeUnit.SECONDS);
        }
        return System.nanoTime() - start;
    }

    private HttpClient newClient() {
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    }

    private HttpResponse<Void> put(HttpClient client, String key, long bytes) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create(sink + "/" + key))
                        .header("Content-Type", "application/octet-stream")
                        // 길이를 박아 Content-Length 로 나가게 한다. ofInputStream 만 쓰면 길이를
                        // 모른다고 보고 chunked 로 나가는데, 실제 S3 presigned PUT 은 길이를 요구한다.
                        .PUT(HttpRequest.BodyPublishers.fromPublisher(
                                HttpRequest.BodyPublishers.ofInputStream(() -> new ZeroInputStream(bytes)),
                                bytes))
                        .build(),
                HttpResponse.BodyHandlers.discarding());
    }

    // --- 리포트 -------------------------------------------------------------

    private record Row(int sizeMb, long singleNanos, long[] parallelNanos) {
    }

    private String report(List<Row> rows) {
        List<String> lines = new ArrayList<>();
        String bar = "──────────────────────────────────────────────────────────────────────";
        lines.add("");
        lines.add(bar);
        lines.add(String.format(" 단일 PUT 대 멀티파트 병렬 (파트 %dMB, 링크 지연 %s)", PART_MB, rttLabel));
        lines.add(bar);

        StringBuilder head = new StringBuilder(String.format("  %-8s %12s", "파일", "단일 PUT"));
        for (int c : CONCURRENCY) {
            head.append(String.format(" %12s", "병렬 " + c));
        }
        lines.add(head.toString());

        for (Row row : rows) {
            String name = row.sizeMb >= 1024
                    ? String.format("%.0f GB", row.sizeMb / 1024.0)
                    : row.sizeMb + " MB";
            StringBuilder time = new StringBuilder(String.format("  %-8s %12s", name, s(row.singleNanos)));
            StringBuilder gain = new StringBuilder(String.format("  %-8s %12s", "", ""));
            for (int c = 0; c < CONCURRENCY.length; c++) {
                time.append(String.format(" %12s", s(row.parallelNanos[c])));
                gain.append(String.format(" %12s",
                        String.format("%.2f배", (double) row.singleNanos / row.parallelNanos[c])));
            }
            lines.add(time.toString());
            lines.add(gain.toString());
        }

        lines.add(bar);
        lines.add(String.format("  %d회 반복 중앙값. 파트마다 커넥션을 따로 연다.", ROUNDS));
        lines.add("  지연은 도커 컨테이너 안에서 tc netem 으로 건 실제 커널 지연이다.");
        lines.add("  커넥션 하나의 처리량은 대략 윈도우 나누기 RTT 라, 지연이 클수록 병렬 이득이 커진다.");
        lines.add(bar);
        return String.join(System.lineSeparator(), lines);
    }

    private static String s(long nanos) {
        return String.format("%.2f s", nanos / 1_000_000_000.0);
    }

    private static long median(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    /** 지정한 크기만큼 0 을 흘려보내는 스트림. 큰 파일을 메모리나 디스크에 올리지 않으려고 쓴다. */
    private static final class ZeroInputStream extends InputStream {
        private long remaining;
        private final AtomicLong counter;

        ZeroInputStream(long size) {
            this(size, null);
        }

        ZeroInputStream(long size, AtomicLong counter) {
            this.remaining = size;
            this.counter = counter;
        }

        @Override
        public int read() {
            byte[] one = new byte[1];
            return read(one, 0, 1) == -1 ? -1 : 0;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (remaining <= 0) return -1;
            int n = (int) Math.min(len, remaining);
            Arrays.fill(b, off, off + n, (byte) 0);
            remaining -= n;
            if (counter != null) counter.addAndGet(n);
            return n;
        }
    }
}
