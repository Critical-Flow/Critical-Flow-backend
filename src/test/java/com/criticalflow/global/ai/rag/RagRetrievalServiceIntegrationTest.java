package com.criticalflow.global.ai.rag;

import org.junit.jupiter.api.*;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("integration-test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RagRetrievalServiceIntegrationTest {

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

    // ── 테스트 고정 ID ─────────────────────────────────────────────────────────

    private static final Long USER_A    = 1L;
    private static final Long USER_B    = 2L;
    private static final Long NOTE_1    = 1L;
    private static final Long NOTE_2    = 2L;
    private static final Long NOTE_3    = 3L;
    private static final Long NOTE_EXCL = 99L;

    private static final List<String> ALL_TEST_IDS =
            List.of("note-1", "note-2", "note-3");

    // ── 주입 ──────────────────────────────────────────────────────────────────

    @Autowired private VectorStore vectorStore;
    @Autowired private RagRetrievalService ragRetrievalService;

    // ── 픽스처 ────────────────────────────────────────────────────────────────

    @BeforeEach
    void cleanUp() {
        vectorStore.delete(ALL_TEST_IDS);
    }

    @AfterEach
    void tearDown() {
        vectorStore.delete(ALL_TEST_IDS);
    }

    private void embed(Long noteId, Long userId, String content, String title) {
        String docId = "note-" + noteId;
        vectorStore.delete(List.of(docId));
        vectorStore.add(List.of(new Document(docId, content, Map.of(
                "note_id",    noteId.toString(),
                "user_id",    userId.toString(),
                "session_id", "1",
                "title",      title,
                "created_at", "2026-01-01"
        ))));
    }

    // ── [user_id 격리] ────────────────────────────────────────────────────────
    // Dense 검색 보장을 위해 문서 content = 쿼리 텍스트 (코사인 유사도 = 1.0)

    @Test
    @Order(1)
    @DisplayName("[user_id 격리] 사용자 A의 노트만 검색되고 사용자 B의 노트는 제외된다")
    void onlyUserADocumentsReturned() {
        embed(NOTE_1, USER_A, "HashMap", "userA-doc");
        embed(NOTE_2, USER_B, "HashMap", "userB-doc");

        RagContext result = ragRetrievalService.retrieve("HashMap", USER_A, NOTE_EXCL);

        assertThat(result.getChunks())
                .isNotEmpty()
                .allMatch(c -> c.getTitle().equals("userA-doc"));
    }

    @Test
    @Order(2)
    @DisplayName("[user_id 격리] 사용자 B로 검색하면 사용자 A의 노트는 반환되지 않는다")
    void userBCannotSeeUserADocuments() {
        embed(NOTE_1, USER_A, "HashMap", "userA-doc");

        RagContext result = ragRetrievalService.retrieve("HashMap", USER_B, NOTE_EXCL);

        assertThat(result.getChunks())
                .noneMatch(c -> c.getTitle().equals("userA-doc"));
    }

    // ── [excludeNoteId 필터] ──────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("[excludeNoteId] 지정한 노트는 결과에서 제외된다")
    void excludedNoteNotInResult() {
        embed(NOTE_1, USER_A, "HashMap", "included");
        embed(NOTE_2, USER_A, "HashMap", "excluded");

        RagContext result = ragRetrievalService.retrieve("HashMap", USER_A, NOTE_2);

        assertThat(result.getChunks()).noneMatch(c -> c.getTitle().equals("excluded"));
        assertThat(result.getChunks()).anyMatch(c -> c.getTitle().equals("included"));
    }

    // ── [Sparse 검색 — threshold=0.0] ─────────────────────────────────────────
    // Dense miss 상황(문서 content ≠ 쿼리)에서 Sparse 키워드 매칭 동작을 검증

    @Test
    @Order(4)
    @DisplayName("[Sparse] threshold=0.0 검색이 키워드 포함 문서를 후보로 가져온다")
    void sparseReturnsKeywordMatchingDocument() {
        // "factorial study notes" → 해시 벡터 기반으로 쿼리 "factorial"과 유사도 < 0.75 가능성 높음
        // Sparse는 키워드 "factorial" 포함 여부로 찾음
        embed(NOTE_1, USER_A, "factorial study notes algorithm", "factorial-doc");
        embed(NOTE_2, USER_A, "tree traversal bfs dfs graph", "unrelated-doc");

        RagContext result = ragRetrievalService.retrieve("factorial", USER_A, NOTE_EXCL);

        assertThat(result.getChunks()).anyMatch(c -> c.getTitle().equals("factorial-doc"));
        assertThat(result.getChunks()).noneMatch(c -> c.getTitle().equals("unrelated-doc"));
    }

    @Test
    @Order(5)
    @DisplayName("[Sparse] topK가 컬렉션 크기보다 크면 존재하는 문서만 반환하고 오류가 없다")
    void topKLargerThanCollectionSizeReturnsAllDocs() {
        // bm25MaxResults=10이지만 문서가 2개뿐 → 오류 없이 동작해야 함
        embed(NOTE_1, USER_A, "HashMap", "doc1");
        embed(NOTE_2, USER_A, "HashMap", "doc2");

        assertThatCode(() -> ragRetrievalService.retrieve("HashMap", USER_A, NOTE_EXCL))
                .doesNotThrowAnyException();
    }

    // ── [하이브리드 보완] ─────────────────────────────────────────────────────
    // Dense + Sparse 중 하나라도 찾으면 결과에 포함됨을 검증
    // Dense miss/보완 여부 자체는 단위 테스트(RagRetrievalServiceTest)에서 검증

    @Test
    @Order(6)
    @DisplayName("[하이브리드] Dense 또는 Sparse 중 하나로 문서를 반환한다")
    void hybridReturnsDocumentViaDenseOrSparse() {
        // content = query → 코사인 유사도 1.0 → Dense가 반드시 찾음
        embed(NOTE_1, USER_A, "factorial", "hybrid-doc");

        RagContext result = ragRetrievalService.retrieve("factorial", USER_A, NOTE_EXCL);

        assertThat(result.getChunks())
                .anyMatch(c -> c.getTitle().equals("hybrid-doc"));
    }

    // ── 결정론적 해시 EmbeddingModel ──────────────────────────────────────────
    // OpenAI API 호출 없이 ChromaDB filterExpression / threshold / topK를 검증하기 위해
    // SHA-256 해시로 384차원 단위벡터를 생성한다.
    // 서로 다른 텍스트의 코사인 유사도 ≈ 0 → Dense threshold=0.75 자연히 미달

    @TestConfiguration
    static class MockEmbeddingConfig {

        @Bean
        @Primary
        EmbeddingModel mockEmbeddingModel() {
            return new HashEmbeddingModel();
        }

        static class HashEmbeddingModel implements EmbeddingModel {

            private static final int DIMS = 384;

            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> embeddings = new ArrayList<>();
                for (int i = 0; i < request.getInstructions().size(); i++) {
                    float[] vec = deterministicVector(request.getInstructions().get(i));
                    embeddings.add(new Embedding(vec, i));
                }
                return new EmbeddingResponse(embeddings);
            }

            @Override
            public float[] embed(Document document) {
                return deterministicVector(document.getText() != null ? document.getText() : "");
            }

            @Override
            public int dimensions() { return DIMS; }

            private float[] deterministicVector(String text) {
                try {
                    byte[] hash = MessageDigest.getInstance("SHA-256")
                            .digest(text.getBytes(StandardCharsets.UTF_8));
                    long seed = ByteBuffer.wrap(hash).getLong();
                    Random rng = new Random(seed);
                    float[] vec = new float[DIMS];
                    for (int i = 0; i < DIMS; i++) vec[i] = rng.nextFloat() - 0.5f;
                    return normalize(vec);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }

            private float[] normalize(float[] vec) {
                float norm = 0f;
                for (float v : vec) norm += v * v;
                norm = (float) Math.sqrt(norm);
                float[] result = new float[DIMS];
                for (int i = 0; i < DIMS; i++) result[i] = vec[i] / norm;
                return result;
            }
        }
    }
}
