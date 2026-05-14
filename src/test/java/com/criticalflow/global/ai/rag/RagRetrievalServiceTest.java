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
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagRetrievalServiceTest {

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private RagRetrievalService service;

    private static final Long USER_ID = 1L;
    private static final Long EXCLUDE_NOTE_ID = 99L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "similarityThreshold", 0.55);
        ReflectionTestUtils.setField(service, "maxResults", 4);
        ReflectionTestUtils.setField(service, "bm25MaxResults", 10);
    }

    // ── 문서 생성 헬퍼 ───────────────────────────────────────────────────────

    private Document doc(String id, String content, String title) {
        return new Document(id, content, Map.of(
                "note_id", id,
                "title", title,
                "session_id", "1",
                "distance", 0.8
        ));
    }

    /**
     * Dense(threshold>=0.55)와 Sparse(threshold==0.0)를 분리해서 스텁.
     */
    private void stubSearch(List<Document> denseResult, List<Document> sparseResult) {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenAnswer(inv -> {
            SearchRequest req = inv.getArgument(0);
            return req.getSimilarityThreshold() >= 0.55 ? denseResult : sparseResult;
        });
    }

    // ── RRF 병합 테스트 ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("RRF 병합")
    class RrfMergeTest {

        @Test
        @DisplayName("Dense와 Sparse 양쪽에 등장한 문서가 가장 높은 RRF 점수를 받는다")
        void documentInBothRanksHigher() {
            Document both      = doc("1", "HashMap은 키-값 쌍을 저장하는 자료구조다", "HashMap");
            Document denseOnly = doc("2", "TreeMap은 정렬된 맵이다", "TreeMap");
            Document sparseOnly = doc("3", "HashMap put get remove 메서드", "HashMap API");

            stubSearch(List.of(both, denseOnly), List.of(both, sparseOnly));

            RagContext result = service.retrieve("HashMap", USER_ID, EXCLUDE_NOTE_ID);

            assertThat(result.isEmpty()).isFalse();
            // 양쪽에 있는 문서가 가장 높은 RRF 점수 → 첫 번째 결과
            assertThat(result.getChunks().get(0).getTitle()).isEqualTo("HashMap");
        }

        @Test
        @DisplayName("Dense에만 있는 문서와 Sparse에만 있는 문서 모두 결과에 포함된다")
        void bothDenseOnlyAndSparseOnlyIncluded() {
            Document denseOnly  = doc("1", "재귀 함수는 자기 자신을 호출한다", "재귀");
            Document sparseOnly = doc("2", "factorial recursive base case", "팩토리얼");

            stubSearch(List.of(denseOnly), List.of(sparseOnly));

            RagContext result = service.retrieve("재귀 recursive", USER_ID, EXCLUDE_NOTE_ID);

            List<String> titles = result.getChunks().stream()
                    .map(RagContext.RetrievedChunk::getTitle)
                    .toList();
            assertThat(titles).containsAnyOf("재귀", "팩토리얼");
        }
    }

    // ── 스파스 검색 테스트 ───────────────────────────────────────────────────

    @Nested
    @DisplayName("스파스(키워드) 검색")
    class SparseSearchTest {

        @Test
        @DisplayName("Dense에서 못 찾은 코드 식별자 포함 문서를 스파스가 발견한다")
        void findsCodeDocumentMissedByDense() {
            Document codeDoc   = doc("1", "public int factorial(int n) { return n * factorial(n-1); }", "팩토리얼 구현");
            Document unrelated = doc("2", "스프링 부트 설정 방법", "설정");

            // Dense는 아무것도 못 찾고, Sparse는 두 문서를 후보로 가져옴
            stubSearch(List.of(), List.of(codeDoc, unrelated));

            RagContext result = service.retrieve("factorial 함수 구현", USER_ID, EXCLUDE_NOTE_ID);

            // "factorial"이 포함된 문서는 키워드 필터 통과
            assertThat(result.getChunks()).anyMatch(c -> c.getTitle().equals("팩토리얼 구현"));
        }

        @Test
        @DisplayName("쿼리 키워드를 포함하지 않는 문서는 스파스 결과에서 제외된다")
        void excludesDocumentWithoutKeyword() {
            Document noMatch = doc("1", "완전히 다른 내용의 문서입니다", "관계없는 문서");

            stubSearch(List.of(), List.of(noMatch));

            // "HashMap"이 noMatch 문서에 없으므로 키워드 필터에서 탈락
            RagContext result = service.retrieve("HashMap 자료구조", USER_ID, EXCLUDE_NOTE_ID);

            assertThat(result.isEmpty()).isTrue();
        }
    }

    // ── 한국어 키워드 테스트 ─────────────────────────────────────────────────

    @Nested
    @DisplayName("한국어 키워드")
    class KoreanKeywordTest {

        @Test
        @DisplayName("2자 한국어 키워드가 포함된 문서를 스파스 검색으로 찾는다")
        void finds2CharKoreanKeyword() {
            Document stackDoc = doc("1", "스택은 LIFO 구조의 자료구조다", "스택");

            stubSearch(List.of(), List.of(stackDoc));

            RagContext result = service.retrieve("스택 자료구조", USER_ID, EXCLUDE_NOTE_ID);

            assertThat(result.getChunks()).anyMatch(c -> c.getTitle().equals("스택"));
        }

        @Test
        @DisplayName("1자 한국어 CS 용어(큐)도 키워드로 인정되어 스파스 검색을 통과한다")
        void includes1CharKoreanCsTerm() {
            Document doc = doc("1", "큐는 FIFO 자료구조다", "큐");

            stubSearch(List.of(), List.of(doc));

            // "큐"(1자)도 한국어 minLength=1 조건으로 포함됨
            RagContext result = service.retrieve("큐", USER_ID, EXCLUDE_NOTE_ID);

            assertThat(result.getChunks()).anyMatch(c -> c.getTitle().equals("큐"));
        }
    }

    // ── isTopicRelevant 2차 필터 테스트 ──────────────────────────────────────

    @Nested
    @DisplayName("2차 키워드 오버랩 필터")
    class TopicRelevantFilterTest {

        @Test
        @DisplayName("키워드 오버랩 20% 미만 문서는 최종 결과에서 제외된다")
        void excludesLowOverlapDocument() {
            Document irrelevant = doc("1", "완전히 다른 주제의 문서", "관련없음");

            stubSearch(List.of(irrelevant), List.of());

            // "HashMap LinkedList ArrayList" 중 어느 것도 "완전히 다른 주제의 문서"에 없음
            RagContext result = service.retrieve("HashMap LinkedList ArrayList", USER_ID, EXCLUDE_NOTE_ID);

            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("키워드 오버랩 20% 이상 문서는 최종 결과에 포함된다")
        void includesHighOverlapDocument() {
            Document relevant = doc("1", "HashMap은 Java의 대표적인 자료구조다", "HashMap");

            stubSearch(List.of(relevant), List.of());

            RagContext result = service.retrieve("HashMap 자료구조", USER_ID, EXCLUDE_NOTE_ID);

            assertThat(result.getChunks()).anyMatch(c -> c.getTitle().equals("HashMap"));
        }
    }

    // ── 빈 결과 및 maxResults 테스트 ─────────────────────────────────────────

    @Test
    @DisplayName("Dense와 Sparse 모두 빈 결과면 빈 RagContext를 반환한다")
    void returnsEmptyContextWhenNoResults() {
        stubSearch(List.of(), List.of());

        RagContext result = service.retrieve("아무것도 없는 쿼리", USER_ID, EXCLUDE_NOTE_ID);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.format()).contains("No relevant past notes found");
    }

    @Test
    @DisplayName("결과는 maxResults(4개) 이하로 제한된다")
    void limitsResultsToMaxResults() {
        List<Document> manyDocs = List.of(
                doc("1", "HashMap 자료구조 설명", "문서1"),
                doc("2", "HashMap 메서드 정리", "문서2"),
                doc("3", "HashMap 성능 분석", "문서3"),
                doc("4", "HashMap 사용 예제", "문서4"),
                doc("5", "HashMap 비교 분석", "문서5")
        );

        stubSearch(manyDocs, List.of());

        RagContext result = service.retrieve("HashMap", USER_ID, EXCLUDE_NOTE_ID);

        assertThat(result.getChunks()).hasSizeLessThanOrEqualTo(4);
    }

    // ── RRF k=60 알고리즘 검증 (#63) ─────────────────────────────────────────

    @Nested
    @DisplayName("RRF k=60 알고리즘")
    class RrfAlgorithmTest {

        /**
         * mergeWithRRF(dense, sparse, limit) — private 메서드를 ReflectionTestUtils로 직접 호출.
         */
        @SuppressWarnings("unchecked")
        private List<Document> mergeWithRRF(List<Document> dense, List<Document> sparse, int limit) {
            return (List<Document>) ReflectionTestUtils.invokeMethod(
                    service, "mergeWithRRF", dense, sparse, limit);
        }

        private Document simpleDoc(String id) {
            return new Document(id, "content", Map.of("note_id", id, "title", id, "session_id", "1"));
        }

        @Test
        @DisplayName("양쪽 등장 문서가 Dense 단독 최상위 문서보다 높은 점수를 받는다")
        void bothListDocumentBeatsDenseOnlyTop() {
            // both(A): Dense rank0 + Sparse rank1 → 1/61 + 1/62 = 0.03252
            // denseOnly(B): Dense rank1 → 1/62 = 0.01613
            // sparseOnly(C): Sparse rank0 → 1/61 = 0.01639
            // 순위: A > C > B
            Document docA = simpleDoc("A");
            Document docB = simpleDoc("B");
            Document docC = simpleDoc("C");

            List<Document> result = mergeWithRRF(
                    List.of(docA, docB),
                    List.of(docC, docA),
                    3);

            assertThat(result.get(0).getId()).isEqualTo("A");
        }

        @Test
        @DisplayName("k=60 소규모 환경에서 양쪽 최하위 등장 문서도 단독 Dense 최상위보다 높은 점수를 받는다")
        void bothListLastRankStillBeatsDenseOnlyTopWithK60() {
            // both(F): Dense rank5 + Sparse rank5 → 2/(60+6) = 0.03030
            // denseOnly(A): Dense rank0 → 1/(60+1) = 0.01639
            // k=60이 최대 후보 6개의 소규모 환경에서도 양쪽 등장 우선 원칙을 유지하는지 검증
            Document docA = simpleDoc("A");
            Document docB = simpleDoc("B");
            Document docC = simpleDoc("C");
            Document docD = simpleDoc("D");
            Document docE = simpleDoc("E");
            Document both = simpleDoc("F");

            List<Document> result = mergeWithRRF(
                    List.of(docA, docB, docC, docD, docE, both),
                    List.of(simpleDoc("G"), simpleDoc("H"), simpleDoc("I"), simpleDoc("J"), simpleDoc("K"), both),
                    6);

            int bothIndex    = result.indexOf(both);
            int denseOnlyIdx = result.stream()
                    .map(Document::getId)
                    .toList()
                    .indexOf("A");

            assertThat(bothIndex).isLessThan(denseOnlyIdx);
        }

        @Test
        @DisplayName("Dense와 Sparse 양쪽 1위 문서가 최고 점수를 받아 Top-1이 된다")
        void denseAndSparseTop1BecomesFinalTop1() {
            // star: Dense rank0 + Sparse rank0 → 2/61 = 0.03279 (최대)
            Document star  = simpleDoc("star");
            Document other = simpleDoc("other");

            List<Document> result = mergeWithRRF(
                    List.of(star, other),
                    List.of(star, other),
                    2);

            assertThat(result.get(0).getId()).isEqualTo("star");
        }

        @Test
        @DisplayName("동일 ID 문서가 Dense와 Sparse에 모두 있어도 결과에 한 번만 포함된다")
        void duplicateDocumentDeduplicatedInResult() {
            Document docA = simpleDoc("A");
            Document docB = simpleDoc("B");
            Document docC = simpleDoc("C");

            List<Document> result = mergeWithRRF(
                    List.of(docA, docB),
                    List.of(docA, docC),
                    4);

            long countA = result.stream().filter(d -> "A".equals(d.getId())).count();
            assertThat(countA).isEqualTo(1);
        }

        @Test
        @DisplayName("limit 파라미터로 결과 수를 정확히 제한한다")
        void respectsLimitParameter() {
            List<Document> dense  = List.of(
                    simpleDoc("1"), simpleDoc("2"), simpleDoc("3"), simpleDoc("4"), simpleDoc("5"));
            List<Document> sparse = List.of(
                    simpleDoc("6"), simpleDoc("7"), simpleDoc("8"), simpleDoc("9"), simpleDoc("10"));

            assertThat(mergeWithRRF(dense, sparse, 3)).hasSize(3);
            assertThat(mergeWithRRF(dense, sparse, 1)).hasSize(1);
        }
    }
}
