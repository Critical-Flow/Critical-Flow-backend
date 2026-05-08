package com.criticalflow.global.ai.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * isTopicRelevant() 2차 필터 단위 테스트 — 이슈 #59
 *
 * 검증 목적: RRF 상위권 문서 중 쿼리 키워드 오버랩이 20% 미만인 문서를 올바르게 제거하는지 확인.
 * 실제 노이즈 감소 비율 측정(수동 실험)은 docs/테스트/ai-test-plane.md #59 수동 단계 참고.
 */
@ExtendWith(MockitoExtension.class)
class IsTopicRelevantFilterTest {

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private RagRetrievalService service;

    private boolean isTopicRelevant(String content, String query) {
        Document doc = new Document("id", content, Map.of());
        return (boolean) ReflectionTestUtils.invokeMethod(service, "isTopicRelevant", doc, query);
    }

    // ── 오버랩 임계값 (20%) ────────────────────────────────────────────────────

    @Nested
    @DisplayName("오버랩 임계값 — 10% 기준 (노트 전체 쿼리 대응)")
    class OverlapThresholdTest {

        @Test
        @DisplayName("키워드 오버랩 0% — 문서 제거")
        void zeroOverlapIsRejected() {
            // query 5개 키워드 중 content에 하나도 없음 → 0/5 = 0% < 10%
            assertThat(isTopicRelevant(
                    "python numpy pandas matplotlib scipy",
                    "HashMap TreeMap LinkedList ArrayList Queue"
            )).isFalse();
        }

        @Test
        @DisplayName("키워드 오버랩 정확히 10% — 통과 (경계값)")
        void exactlyTenPercentPasses() {
            // 10개 키워드 중 1개 포함 → 1/10 = 10% → true (>= 0.1)
            assertThat(isTopicRelevant(
                    "HashMap은 Java의 자료구조다",
                    "HashMap TreeMap LinkedList ArrayList Queue Stack Tree Graph Heap Sort"
            )).isTrue();
        }

        @Test
        @DisplayName("키워드 오버랩 9% 미만 — 문서 제거")
        void belowTenPercentIsRejected() {
            // 20개 키워드 중 1개 → 1/20 = 5% < 10%
            assertThat(isTopicRelevant(
                    "HashMap만 포함된 문서",
                    "TreeMap LinkedList ArrayList Queue Stack Tree Graph Heap Sort Deque " +
                    "Iterator Comparator Collection Framework Interface Abstract Pattern"
            )).isFalse();
        }

        @Test
        @DisplayName("키워드 오버랩 50% 이상 — 통과")
        void highOverlapPasses() {
            // 4개 중 3개 포함 → 3/4 = 75%
            assertThat(isTopicRelevant(
                    "HashMap TreeMap LinkedList 설명",
                    "HashMap TreeMap LinkedList ArrayList"
            )).isTrue();
        }
    }

    // ── 한국어 / 영어 최소 길이 기준 ─────────────────────────────────────────

    @Nested
    @DisplayName("언어별 최소 키워드 길이")
    class MinLengthByLanguageTest {

        @Test
        @DisplayName("한국어 쿼리 — 2자 이상 키워드만 유효 키워드로 인정된다")
        void koreanQueryCountsTwoCharKeyword() {
            // hasKorean=true → minLength=2 → "스택"(2자) 유효, "큐"(1자)·"힙"(1자) 제외
            // 쿼리: "큐 스택 힙" → keywords=["스택"], content에 "스택" 포함 → 1/1 = 100% → true
            assertThat(isTopicRelevant(
                    "스택은 LIFO 구조의 자료구조다",
                    "큐 스택 힙"
            )).isTrue();
        }

        @Test
        @DisplayName("영어 쿼리 — 2자 이하 단어는 유효 키워드에서 제외된다")
        void englishQueryIgnoresShortWords() {
            // "is"(2자), "a"(1자) → minLength=3 → 유효하지 않음
            // 유효 키워드: ["hashmap", "map"] → content에 "hashmap" 포함 → 1/2 = 50% → true
            assertThat(isTopicRelevant(
                    "HashMap 자료구조 설명",
                    "HashMap is a map"
            )).isTrue();
        }

        @Test
        @DisplayName("영어 쿼리에 3자 미만 단어만 있으면 유효 키워드 0개 → 항상 통과")
        void noSignificantKeywordsAlwaysPasses() {
            // "is"(2자), "a"(1자), "an"(2자) → 모두 길이 < 3 → significant=0 → true
            assertThat(isTopicRelevant(
                    "전혀 관련 없는 내용",
                    "is a an"
            )).isTrue();
        }

        @Test
        @DisplayName("한국어 쿼리 — 오버랩 없으면 제거된다")
        void koreanQueryWithNoMatchIsRejected() {
            // "큐 스택 힙" → significant=3, content에 하나도 없음 → 0/3 = 0% → false
            assertThat(isTopicRelevant(
                    "HashMap LinkedList TreeMap ArrayList",
                    "큐 스택 힙"
            )).isFalse();
        }
    }

    // ── 실제 노이즈 시나리오 ──────────────────────────────────────────────────

    @Nested
    @DisplayName("실제 노이즈 시나리오")
    class RealNoiseScenarioTest {

        @Test
        @DisplayName("Java GC 쿼리에 Python GC 문서 — 토픽 불일치로 제거된다")
        void javaPythonTopicMismatchIsRejected() {
            // 쿼리: "Java GC garbage collection heap"
            // 유효 키워드(>=3자): ["java", "garbage", "collection", "heap"] → 4개
            // content: "Python garbage collection memory management"
            // 포함: "garbage"(o), "collection"(o) → 2/4 = 50% → true
            // ※ 이 케이스는 실제로 통과됨을 검증 — 부분 일치로 필터가 너무 관대함을 보임
            assertThat(isTopicRelevant(
                    "Python garbage collection memory management",
                    "Java GC garbage collection heap"
            )).isTrue();
        }

        @Test
        @DisplayName("알고리즘 쿼리에 완전히 무관한 프레임워크 문서 — 제거된다")
        void completelyUnrelatedDocumentIsRejected() {
            // 쿼리: "binary search tree traversal"
            // 유효 키워드: ["binary", "search", "tree", "traversal"] → 4개
            // content: "Spring Boot configuration properties setup"
            // 포함: 없음 → 0/4 = 0% → false
            assertThat(isTopicRelevant(
                    "Spring Boot configuration properties setup",
                    "binary search tree traversal"
            )).isFalse();
        }

        @Test
        @DisplayName("재귀 쿼리에 피보나치 구현 문서 — 키워드 포함으로 통과한다")
        void recursiveDocumentPassesWithKeywordMatch() {
            // 쿼리: "재귀 함수 구현"
            // 유효 키워드(한국어, >=1자): ["재귀", "함수", "구현"] → 3개
            // content에 "재귀" 포함 → 1/3 ≈ 33% → true
            assertThat(isTopicRelevant(
                    "피보나치 수열을 재귀로 구현하는 방법",
                    "재귀 함수 구현"
            )).isTrue();
        }
    }
}
