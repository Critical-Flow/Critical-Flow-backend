package com.criticalflow.global.ai.router;

import com.criticalflow.domain.conversation.entity.QuestionType;
import com.criticalflow.domain.note.entity.StudyNote;
import com.criticalflow.global.ai.rag.RagContext;
import org.junit.jupiter.api.*;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QuestionTypeRouter LLM 통합 테스트
 *
 * LLM이 TYPE A~F를 의도대로 분류하는지 검증.
 * TYPE_E는 코드 블록 규칙 필터로 처리되므로 LLM 테스트 제외.
 * 실제 OpenAI API 호출 — CI 환경에서는 제외.
 */
@SpringBootTest
@ActiveProfiles("llm-router-test")
@Tag("llm-integration")
@DisplayName("QuestionTypeRouter LLM 분류 정확도 테스트")
class QuestionTypeRouterLlmTest {

    @Autowired
    private QuestionTypeRouter router;

    @MockBean
    private VectorStore vectorStore; // ChromaDB 연결 없이 테스트

    // 결과 집계
    private static final Map<String, List<QuestionType>> results = new LinkedHashMap<>();
    private static int clearCorrect = 0;
    private static int clearTotal = 0;

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private StudyNote note(String content) {
        return StudyNote.builder()
                .noteId(999L).userId(1L).sessionId(1L)
                .title("테스트 노트").content(content)
                .isSaved(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private RagContext emptyRag() {
        return RagContext.builder().chunks(List.of()).build();
    }

    private RagContext nonEmptyRag() {
        return RagContext.builder()
                .chunks(List.of(RagContext.RetrievedChunk.builder()
                        .title("과거 학습 노트").sessionId("1").score(0.8)
                        .content("관련 과거 학습 내용입니다.").build()))
                .build();
    }

    /** 동일 노트를 3회 호출하여 일관성을 확인한다. */
    private QuestionType routeWithConsistency(String label, String content, RagContext rag, QuestionType expected) {
        List<QuestionType> runs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            runs.add(router.route(note(content), rag));
        }
        results.put(label, runs);

        long consistent = runs.stream().filter(r -> r == runs.get(0)).count();
        System.out.printf("[%s] 결과: %s | 일관성: %d/3 | 기대: %s%n",
                label, runs, consistent, expected);

        return runs.stream()
                .collect(java.util.stream.Collectors.groupingBy(r -> r, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(runs.get(0));
    }

    // ── TYPE A — 개념 정의 확인 ──────────────────────────────────────────────

    @Test @DisplayName("TYPE_A: JWT 개념 정의")
    void typeA_JWT() {
        QuestionType r = routeWithConsistency("TYPE_A_JWT",
                "JWT는 Header.Payload.Signature 세 부분으로 구성된다. 각 부분은 Base64로 인코딩된다.",
                emptyRag(), QuestionType.TYPE_A);
        assertThat(r).isEqualTo(QuestionType.TYPE_A);
        updateClear(r == QuestionType.TYPE_A);
    }

    @Test @DisplayName("TYPE_A: TCP 개념 정의")
    void typeA_TCP() {
        QuestionType r = routeWithConsistency("TYPE_A_TCP",
                "TCP는 연결 지향 프로토콜이다. 데이터 전송 전에 3-way handshake로 연결을 확립한다.",
                emptyRag(), QuestionType.TYPE_A);
        assertThat(r).isEqualTo(QuestionType.TYPE_A);
        updateClear(r == QuestionType.TYPE_A);
    }

    @Test @DisplayName("TYPE_A: 빅오 표기법 정의")
    void typeA_BigO() {
        QuestionType r = routeWithConsistency("TYPE_A_BigO",
                "빅오 표기법이란 알고리즘의 시간복잡도를 나타내는 방법으로, 입력 크기에 따른 최악의 성능을 표현한다.",
                emptyRag(), QuestionType.TYPE_A);
        assertThat(r).isEqualTo(QuestionType.TYPE_A);
        updateClear(r == QuestionType.TYPE_A);
    }

    // ── TYPE B — 설계 의도 탐구 ──────────────────────────────────────────────

    @Test @DisplayName("TYPE_B: Redis vs Memcached 선택 근거")
    void typeB_Redis() {
        QuestionType r = routeWithConsistency("TYPE_B_Redis",
                "Redis를 캐시로 선택했다. Memcached도 고려했지만 데이터 영속성과 다양한 자료구조 지원 때문에 Redis로 결정했다.",
                emptyRag(), QuestionType.TYPE_B);
        assertThat(r).isEqualTo(QuestionType.TYPE_B);
        updateClear(r == QuestionType.TYPE_B);
    }

    @Test @DisplayName("TYPE_B: PostgreSQL 선택 이유")
    void typeB_PostgreSQL() {
        QuestionType r = routeWithConsistency("TYPE_B_PostgreSQL",
                "MySQL 대신 PostgreSQL을 쓴 이유는 네이티브 JSON 지원과 복잡한 쿼리 성능 때문이다.",
                emptyRag(), QuestionType.TYPE_B);
        assertThat(r).isEqualTo(QuestionType.TYPE_B);
        updateClear(r == QuestionType.TYPE_B);
    }

    @Test @DisplayName("TYPE_B: GraphQL 선택 이유")
    void typeB_GraphQL() {
        QuestionType r = routeWithConsistency("TYPE_B_GraphQL",
                "REST 대신 GraphQL을 선택한 이유는 오버페칭 문제를 해결하고 클라이언트가 필요한 데이터만 요청할 수 있기 때문이다.",
                emptyRag(), QuestionType.TYPE_B);
        assertThat(r).isEqualTo(QuestionType.TYPE_B);
        updateClear(r == QuestionType.TYPE_B);
    }

    // ── TYPE C — 과거 학습 연계 (rag_available=true) ─────────────────────────

    @Test @DisplayName("TYPE_C: 트랜잭션 격리 수준과 Lock 연계 (RAG 있음)")
    void typeC_Transaction() {
        QuestionType r = routeWithConsistency("TYPE_C_Transaction",
                "오늘 배운 트랜잭션 격리 수준이 지난번에 배운 Lock과 연결되는 것 같다.",
                nonEmptyRag(), QuestionType.TYPE_C);
        assertThat(r).isEqualTo(QuestionType.TYPE_C);
        updateClear(r == QuestionType.TYPE_C);
    }

    @Test @DisplayName("TYPE_C: 스프링 AOP와 프록시 패턴 연계 (RAG 있음)")
    void typeC_AOP() {
        QuestionType r = routeWithConsistency("TYPE_C_AOP",
                "스프링 AOP가 예전에 배운 프록시 패턴과 같은 원리다.",
                nonEmptyRag(), QuestionType.TYPE_C);
        assertThat(r).isEqualTo(QuestionType.TYPE_C);
        updateClear(r == QuestionType.TYPE_C);
    }

    @Test @DisplayName("TYPE_C: 힙과 완전 이진 트리 연계 (RAG 있음)")
    void typeC_Heap() {
        QuestionType r = routeWithConsistency("TYPE_C_Heap",
                "오늘 배운 힙 자료구조가 이전에 배운 완전 이진 트리를 기반으로 한다는 걸 알았다.",
                nonEmptyRag(), QuestionType.TYPE_C);
        assertThat(r).isEqualTo(QuestionType.TYPE_C);
        updateClear(r == QuestionType.TYPE_C);
    }

    @Test @DisplayName("TYPE_C: RAG 없으면 TYPE_A로 폴백")
    void typeC_FallbackToTypeA() {
        QuestionType r = routeWithConsistency("TYPE_C_폴백",
                "오늘 배운 트랜잭션 격리 수준이 지난번에 배운 Lock과 연결되는 것 같다.",
                emptyRag(), QuestionType.TYPE_A);
        assertThat(r).isEqualTo(QuestionType.TYPE_A);
    }

    // ── TYPE D — 심층 사고 탐구 ──────────────────────────────────────────────

    @Test @DisplayName("TYPE_D: 재귀 기저 조건 없을 때 엣지케이스")
    void typeD_Recursion() {
        QuestionType r = routeWithConsistency("TYPE_D_Recursion",
                "재귀 함수는 기저 조건이 없으면 스택 오버플로우가 발생한다.",
                emptyRag(), QuestionType.TYPE_D);
        assertThat(r).isEqualTo(QuestionType.TYPE_D);
        updateClear(r == QuestionType.TYPE_D);
    }

    @Test @DisplayName("TYPE_D: HashMap 해시 충돌 엣지케이스")
    void typeD_HashMap() {
        QuestionType r = routeWithConsistency("TYPE_D_HashMap",
                "HashMap은 평균 O(1)이지만 해시 충돌이 많이 발생하면 최악의 경우 O(n)이 된다.",
                emptyRag(), QuestionType.TYPE_D);
        assertThat(r).isEqualTo(QuestionType.TYPE_D);
        updateClear(r == QuestionType.TYPE_D);
    }

    @Test @DisplayName("TYPE_D: 동기 처리 블로킹 문제")
    void typeD_Sync() {
        QuestionType r = routeWithConsistency("TYPE_D_Sync",
                "동기 처리는 순서가 보장되지만 앞 작업이 끝나야 다음이 시작되어 블로킹 문제가 생긴다.",
                emptyRag(), QuestionType.TYPE_D);
        assertThat(r).isEqualTo(QuestionType.TYPE_D);
        updateClear(r == QuestionType.TYPE_D);
    }

    // ── 경계 케이스 ───────────────────────────────────────────────────────────

    @Test @DisplayName("경계: TYPE_A vs TYPE_D — HashMap 개념+엣지케이스")
    void boundary_A_vs_D() {
        QuestionType r = routeWithConsistency("경계_A_vs_D",
                "HashMap은 평균 O(1)이지만 최악의 경우 O(n)이다.",
                emptyRag(), null);
        boolean acceptable = r == QuestionType.TYPE_A || r == QuestionType.TYPE_D;
        System.out.printf("[경계_A_vs_D] 결과: %s | 허용 범위(A or D): %s%n", r, acceptable);
        assertThat(acceptable).isTrue();
    }

    @Test @DisplayName("경계: TYPE_B vs TYPE_D — 인덱스 트레이드오프+엣지케이스")
    void boundary_B_vs_D() {
        QuestionType r = routeWithConsistency("경계_B_vs_D",
                "인덱스를 걸면 조회는 빠르지만 쓰기 성능이 저하된다.",
                emptyRag(), null);
        boolean acceptable = r == QuestionType.TYPE_B || r == QuestionType.TYPE_D;
        System.out.printf("[경계_B_vs_D] 결과: %s | 허용 범위(B or D): %s%n", r, acceptable);
        assertThat(acceptable).isTrue();
    }

    // ── 최종 집계 ─────────────────────────────────────────────────────────────

    @AfterAll
    static void printSummary() {
        System.out.println("\n========== QuestionTypeRouter LLM 테스트 최종 결과 ==========");
        results.forEach((label, runs) ->
                System.out.printf("  %-25s → %s%n", label, runs));
        System.out.printf("%n명확한 케이스 정확도: %d / %d (합격 기준: 10/12 이상)%n",
                clearCorrect, clearTotal);
        System.out.printf("판정: %s%n",
                clearCorrect >= 10 ? "✅ PASS" : "❌ FAIL");
        System.out.println("=============================================================");
    }

    private synchronized void updateClear(boolean correct) {
        clearTotal++;
        if (correct) clearCorrect++;
    }
}
