package com.criticalflow.global.ai.rag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [#57] NotePreprocessor 전처리 전후 임베딩 품질 정량 비교
 *
 * 실행 방법:
 *   export OPENAI_API_KEY=sk-...
 *   ./gradlew test --tests "*NotePreprocessorEmbeddingQualityTest"
 *
 * 통과 기준: 코드 블록 포함 노트에서 전처리 적용 Recall@4 개선폭 >= 10%p
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("embedding-quality-test")
@Testcontainers
@Tag("embedding-quality")
class NotePreprocessorEmbeddingQualityTest {

    private static final Logger log = LoggerFactory.getLogger(NotePreprocessorEmbeddingQualityTest.class);

    // ── ChromaDB Testcontainer ─────────────────────────────────────────────────

    @Container
    static GenericContainer<?> chromadb = new GenericContainer<>("chromadb/chroma:1.0.0")
            .withExposedPorts(8000)
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void configureChroma(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.vectorstore.chroma.client.host", () -> "http://localhost");
        registry.add("spring.ai.vectorstore.chroma.client.port",
                () -> String.valueOf(chromadb.getMappedPort(8000)));
    }

    // ── 주입 ──────────────────────────────────────────────────────────────────

    @Autowired private VectorStore vectorStore;
    @Autowired private NotePreprocessor notePreprocessor;

    // ── 테스트 데이터 ──────────────────────────────────────────────────────────

    private static final String USER_ID = "quality-test-user";

    record TestCase(String docId, String title, String content, String query) {}
    record QualityMetrics(double recall, double avgDistance, int hits) {}

    private static final List<TestCase> TEST_CASES = List.of(

        new TestCase("qnote-1", "HashMap 자료구조",
            """
            # HashMap 자료구조

            HashMap은 키-값 쌍을 저장하는 자료구조입니다. 평균 O(1) 시간복잡도.

            ```java
            import java.util.HashMap;
            import java.util.Map;

            public class HashMapExample {
                public static void main(String[] args) {
                    HashMap<String, Integer> scores = new HashMap<>();
                    scores.put("Alice", 95);
                    scores.put("Bob", 87);
                    scores.put("Carol", 72);

                    int aliceScore = scores.get("Alice");
                    boolean hasKey = scores.containsKey("Dave");
                    scores.remove("Bob");

                    for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                        System.out.printf("%s: %d%n", entry.getKey(), entry.getValue());
                    }
                }
            }
            ```

            put으로 삽입, get으로 조회, containsKey로 존재 확인합니다.
            """,
            "HashMap put get containsKey 사용법"),

        new TestCase("qnote-2", "재귀 함수 팩토리얼",
            """
            # 재귀 함수 — 팩토리얼과 피보나치

            재귀 함수는 자기 자신을 호출하여 문제를 해결합니다.

            ```python
            def factorial(n):
                if n == 0 or n == 1:
                    return 1
                return n * factorial(n - 1)

            def fibonacci(n, memo={}):
                if n in memo:
                    return memo[n]
                if n <= 1:
                    return n
                memo[n] = fibonacci(n - 1, memo) + fibonacci(n - 2, memo)
                return memo[n]
            ```

            기저 조건(base case)을 반드시 정의해야 무한 재귀를 방지합니다.
            """,
            "재귀 호출 팩토리얼 기저 조건 구현"),

        new TestCase("qnote-3", "이진 탐색 알고리즘",
            """
            # 이진 탐색 알고리즘

            정렬된 배열에서 O(log n) 시간에 원소를 찾습니다.

            ```java
            public class BinarySearch {
                public static int binarySearch(int[] arr, int target) {
                    int left = 0;
                    int right = arr.length - 1;

                    while (left <= right) {
                        int mid = left + (right - left) / 2;
                        if (arr[mid] == target) return mid;
                        else if (arr[mid] < target) left = mid + 1;
                        else right = mid - 1;
                    }
                    return -1;
                }
            }
            ```

            left, right, mid 포인터로 탐색 범위를 절반씩 좁힙니다.
            """,
            "정렬된 배열 이진 탐색 left right mid 구현"),

        new TestCase("qnote-4", "SQL JOIN 쿼리",
            """
            # SQL 조인(JOIN) 쿼리

            여러 테이블을 연결하여 관련 데이터를 조회합니다.

            ```sql
            SELECT u.name, o.product, o.amount
            FROM users u
            INNER JOIN orders o ON u.user_id = o.user_id
            WHERE o.amount > 10000
            ORDER BY o.amount DESC;

            SELECT u.name, COUNT(o.order_id) AS order_count
            FROM users u
            LEFT JOIN orders o ON u.user_id = o.user_id
            GROUP BY u.user_id, u.name
            HAVING COUNT(o.order_id) >= 2;
            ```

            INNER JOIN은 교집합, LEFT JOIN은 왼쪽 기준 합집합입니다.
            """,
            "SQL INNER JOIN LEFT JOIN 테이블 조인 쿼리"),

        new TestCase("qnote-5", "버블 정렬 알고리즘",
            """
            # 버블 정렬(Bubble Sort)

            인접한 원소를 비교·교환하며 정렬합니다. 시간복잡도 O(n²).

            ```java
            public class BubbleSort {
                public static void bubbleSort(int[] arr) {
                    int n = arr.length;
                    for (int i = 0; i < n - 1; i++) {
                        boolean swapped = false;
                        for (int j = 0; j < n - i - 1; j++) {
                            if (arr[j] > arr[j + 1]) {
                                int temp = arr[j];
                                arr[j] = arr[j + 1];
                                arr[j + 1] = temp;
                                swapped = true;
                            }
                        }
                        if (!swapped) break;
                    }
                }
            }
            ```

            swap이 없으면 이미 정렬된 배열로 조기 종료합니다.
            """,
            "버블 정렬 swap 인접 비교 알고리즘 구현"),

        new TestCase("qnote-6", "스프링 의존성 주입",
            """
            # 스프링 의존성 주입(DI)

            스프링 IoC 컨테이너가 빈을 생성하고 주입합니다.

            ```java
            import org.springframework.stereotype.Service;
            import org.springframework.stereotype.Repository;
            import org.springframework.beans.factory.annotation.Autowired;

            @Repository
            public class UserRepository {
                public User findById(Long userId) { return null; }
            }

            @Service
            public class UserService {
                private final UserRepository userRepository;

                @Autowired
                public UserService(UserRepository userRepository) {
                    this.userRepository = userRepository;
                }

                public User getUser(Long userId) {
                    return userRepository.findById(userId);
                }
            }
            ```

            생성자 주입 방식이 테스트 용이성 측면에서 권장됩니다.
            """,
            "스프링 Autowired 빈 의존성 주입 Service Repository"),

        new TestCase("qnote-7", "동적 프로그래밍 메모이제이션",
            """
            # 동적 프로그래밍 — 메모이제이션

            중복 계산을 캐시하여 시간복잡도를 줄입니다.

            ```java
            import java.util.HashMap;
            import java.util.Map;

            public class DynamicProgramming {
                private static final Map<Integer, Long> memo = new HashMap<>();

                public static long fibonacci(int n) {
                    if (n <= 1) return n;
                    if (memo.containsKey(n)) return memo.get(n);
                    long result = fibonacci(n - 1) + fibonacci(n - 2);
                    memo.put(n, result);
                    return result;
                }

                public static int climbStairs(int n) {
                    int[] dp = new int[n + 1];
                    dp[0] = 1;
                    dp[1] = 1;
                    for (int i = 2; i <= n; i++) dp[i] = dp[i - 1] + dp[i - 2];
                    return dp[n];
                }
            }
            ```

            탑다운(메모이제이션)과 바텀업(dp 배열) 두 방식이 있습니다.
            """,
            "메모이제이션 dp 피보나치 동적 프로그래밍 최적화"),

        new TestCase("qnote-8", "BFS 그래프 탐색",
            """
            # BFS — 너비 우선 탐색

            큐를 이용해 가까운 노드부터 순서대로 탐색합니다.

            ```java
            import java.util.*;

            public class BFS {
                public static List<Integer> bfs(Map<Integer, List<Integer>> graph, int start) {
                    List<Integer> visited = new ArrayList<>();
                    Set<Integer> seen = new HashSet<>();
                    Queue<Integer> queue = new LinkedList<>();

                    queue.offer(start);
                    seen.add(start);

                    while (!queue.isEmpty()) {
                        int node = queue.poll();
                        visited.add(node);
                        for (int neighbor : graph.getOrDefault(node, List.of())) {
                            if (!seen.contains(neighbor)) {
                                seen.add(neighbor);
                                queue.offer(neighbor);
                            }
                        }
                    }
                    return visited;
                }
            }
            ```

            최단 경로 탐색에 적합하며 레벨 단위 처리가 가능합니다.
            """,
            "BFS 너비 우선 탐색 큐 Queue LinkedList 구현"),

        new TestCase("qnote-9", "스택 자료구조",
            """
            # 스택(Stack) 자료구조

            LIFO(Last In First Out) 방식으로 동작합니다.

            ```java
            import java.util.ArrayDeque;
            import java.util.Deque;

            public class StackExample {
                public static boolean isBalanced(String s) {
                    Deque<Character> stack = new ArrayDeque<>();
                    for (char c : s.toCharArray()) {
                        if (c == '(' || c == '[' || c == '{') {
                            stack.push(c);
                        } else {
                            if (stack.isEmpty()) return false;
                            char top = stack.pop();
                            if ((c == ')' && top != '(') ||
                                (c == ']' && top != '[') ||
                                (c == '}' && top != '{')) return false;
                        }
                    }
                    return stack.isEmpty();
                }
            }
            ```

            괄호 매칭, 함수 호출 스택, DFS 구현 등에 활용됩니다.
            """,
            "스택 LIFO push pop peek 괄호 매칭 구현"),

        new TestCase("qnote-10", "자바 예외 처리",
            """
            # 자바 예외 처리(Exception Handling)

            try-catch-finally로 예외를 안전하게 처리합니다.

            ```java
            import java.io.*;

            public class ExceptionHandling {
                public static String readFile(String path) {
                    StringBuilder content = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            content.append(line).append("\\n");
                        }
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException("파일 없음: " + path, e);
                    } catch (IOException e) {
                        throw new RuntimeException("읽기 실패", e);
                    } finally {
                        System.out.println("readFile 완료");
                    }
                    return content.toString();
                }
            }
            ```

            try-with-resources로 AutoCloseable 자원을 자동 해제합니다.
            """,
            "자바 예외 처리 try catch finally IOException RuntimeException")
    );

    // ── 픽스처 ────────────────────────────────────────────────────────────────

    private List<String> allDocIds() {
        return TEST_CASES.stream().map(TestCase::docId).toList();
    }

    @BeforeEach
    void cleanUp() {
        vectorStore.delete(allDocIds());
    }

    @AfterEach
    void tearDown() {
        vectorStore.delete(allDocIds());
    }

    // ── 메인 테스트 ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("[#57] NotePreprocessor 전처리 전후 Recall@4 비교")
    void comparePreprocessingEmbeddingQuality() {
        // Phase 1: 원본 임베딩 (전처리 미적용)
        embedPhase(false);
        QualityMetrics without = measureQuality("전처리 미적용");
        vectorStore.delete(allDocIds());

        // Phase 2: 전처리 적용 임베딩
        embedPhase(true);
        QualityMetrics with = measureQuality("전처리 적용");
        vectorStore.delete(allDocIds());

        // 결과 요약
        double recallDiff = with.recall() - without.recall();
        log.info("============================================================");
        log.info("[#57] NotePreprocessor 임베딩 품질 비교 결과");
        log.info("전처리 미적용 — Recall@4: {}%  (히트: {}/{})",
                String.format("%.0f", without.recall() * 100),
                without.hits(), TEST_CASES.size());
        log.info("전처리 적용   — Recall@4: {}%  (히트: {}/{})",
                String.format("%.0f", with.recall() * 100),
                with.hits(), TEST_CASES.size());
        log.info("개선폭: {}pp  |  통과 기준: >= 10pp",
                String.format("%.0f", recallDiff * 100));
        log.info("결과: {}", recallDiff >= 0.1 ? "PASS" : "FAIL");
        log.info("============================================================");

        assertThat(recallDiff)
                .as(String.format(
                        "코드 블록 포함 노트에서 전처리 Recall@4(%.0f%%) - 미적용(%.0f%%) = %.1f%%p (기준 >=10pp)",
                        with.recall() * 100, without.recall() * 100, recallDiff * 100))
                .isGreaterThanOrEqualTo(0.1);
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private void embedPhase(boolean usePreprocessor) {
        List<Document> docs = new ArrayList<>();
        for (TestCase tc : TEST_CASES) {
            String content = usePreprocessor
                    ? notePreprocessor.preprocessForEmbedding(tc.content())
                    : tc.content();
            docs.add(new Document(tc.docId(), content, Map.of(
                    "note_id",    tc.docId(),
                    "user_id",    USER_ID,
                    "session_id", "1",
                    "title",      tc.title(),
                    "created_at", "2026-01-01"
            )));
        }
        vectorStore.add(docs);
    }

    private QualityMetrics measureQuality(String label) {
        int hits = 0;
        double totalDistance = 0.0;
        int distanceCount = 0;

        for (TestCase tc : TEST_CASES) {
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(tc.query())
                            .topK(4)
                            .similarityThreshold(0.0)
                            .filterExpression("user_id == '" + USER_ID + "'")
                            .build()
            );

            Optional<Document> matched = results.stream()
                    .filter(doc -> tc.docId().equals(doc.getId()))
                    .findFirst();

            boolean hit = matched.isPresent();
            if (hit) {
                hits++;
                Object raw = matched.get().getMetadata().get("distance");
                if (raw instanceof Number n) {
                    totalDistance += n.doubleValue();
                    distanceCount++;
                }
            }

            log.info("[{}][{}] 쿼리='{}' | {} | top4={}",
                    label, tc.docId(), tc.query(),
                    hit ? "HIT" : "miss",
                    results.stream().map(Document::getId).toList());
        }

        double avgDistance = distanceCount > 0 ? totalDistance / distanceCount : 0.0;
        return new QualityMetrics((double) hits / TEST_CASES.size(), avgDistance, hits);
    }
}
