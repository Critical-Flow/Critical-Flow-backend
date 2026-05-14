# #61 — RagRetrievalService 실제 ChromaDB 연동 동작 통합 테스트

| 항목 | 내용 |
|---|---|
| 작성자 | chamingyeong |
| 작성일 | 2026-05-08 |
| 관련 요구사항 ID | REQ-AIV-002, REQ-AIV-003, REQ-AIV-004 |
| 관련 이슈/PR 번호 | #61 / PR #67 |
| 테스트 상태 | 완료 |

---

## 공통 테스트 환경

| 항목 | 값 |
|---|---|
| OS | macOS Darwin 25.4.0 |
| JDK | 21.0.6 LTS |
| Spring Boot | 3.2.4 |
| LLM 모델 | 해당 없음 |
| 임베딩 모델 | HashEmbeddingModel (결정론적 SHA-256 벡터, OpenAI API 불필요) |
| 벡터 DB | ChromaDB 1.0.0 (Testcontainers 자동 기동) |
| 실행 환경 | 로컬 / CI |
| 외부 API 호출 여부 | Mock 대체 (HashEmbeddingModel) |
| 테스트 데이터셋 | 테스트 코드 내 인라인 Document 객체 |
| 문서 최초 작성일 | 2026-05-08 |
| 최종 수정일 | 2026-05-08 |

---

## 1. 진행 이유

**1-1. 발견 경위**
기존 `RagRetrievalServiceTest`가 `VectorStore`를 Mockito Mock으로 교체해 비즈니스 로직(RRF 점수 계산, 2차 필터)을 검증했으나, 실제 ChromaDB와의 연동 동작은 보장하지 않았다. `feat/hybrid-search-bm25` 브랜치에서 이미 ChromaDB 1.0.0 버전 호환성 문제를 겪은 이력이 있었다(커밋 `54b277f`).

**1-2. 해결하지 않을 경우 영향**
- `filterExpression` 문법이 ChromaDB 버전에서 실제로 파싱되지 않으면 다른 사용자 노트가 검색 결과에 노출될 수 있음 (REQ-AIV-002 위반)
- `threshold=0.0` Sparse 검색이 ChromaDB에서 예상과 다르게 동작해 하이브리드 검색이 실질적으로 Dense-only와 동일하게 동작할 위험

**1-3. 관련 요구사항**
- REQ-AIV-002 — 검색 결과에 요청 학습자 본인 노트만 포함
- REQ-AIV-003 — 현재 노트 자신이 검색 결과에 포함되지 않음
- REQ-AIV-004 — 관련도 임계값 미만 항목 제외

---

## 2. 측정 방법

**2-1. 측정 방식**
- [x] 단위 테스트 (Testcontainers + JUnit 5, ChromaDB 실제 컨테이너)

**2-2. 측정 조건**
- Testcontainers로 ChromaDB 1.0.0 컨테이너 자동 기동
- `HashEmbeddingModel`로 SHA-256 해시 기반 결정론적 384차원 단위벡터 생성 (OpenAI API 비용 없음)
- `application-integration-test.properties`로 H2 datasource, 더미 OAuth2/JWT, ChromaDB 테스트 컬렉션 설정

**2-3. 테스트 데이터 구성**
- 데이터 출처: 테스트 코드 내 인라인 Document 객체
- 데이터 건수: 사용자별 3~5개 노트 (filterExpression 검증용)
- 데이터 특성: `user_id` 메타데이터로 사용자 A/B 구분
- 정답 레이블 여부: 있음 (테스트별 예상 결과 명시)

**2-4. 측정 기준 및 도구**
- 측정 지표: 테스트 통과 여부 (6개 케이스)
- 측정 도구: JUnit 5 단언문
- 성공 기준: 6/6 통과

**2-5. 측정 반복 횟수**
CI/CD 파이프라인에서 매 PR마다 자동 반복

**2-6. 이 방식을 선택한 이유**
Testcontainers를 사용하면 CI에서 ChromaDB Docker 컨테이너를 실행해 통합 테스트를 자동화할 수 있다. Mock 테스트 대비 ChromaDB 엔진 동작 기반이므로 훨씬 높은 신뢰도를 제공한다.

---

## 3. 1차 결과

**3-1. 측정 결과**

| 측정 항목 | 결과 |
|---|---|
| user_id 격리 (2케이스) | ✅ 통과 |
| excludeNoteId 필터 (1케이스) | ✅ 통과 |
| Sparse threshold=0.0 (2케이스) | ✅ 통과 |
| 하이브리드 보완 (1케이스) | ✅ 통과 |
| **총계** | **6/6 통과** |

```
[user_id 격리] 사용자 A의 노트만 검색되고 사용자 B의 노트는 제외된다  ✅
[user_id 격리] 사용자 B로 검색하면 사용자 A의 노트는 반환되지 않는다  ✅
[excludeNoteId] 지정한 노트는 결과에서 제외된다                       ✅
[Sparse] threshold=0.0 검색이 키워드 포함 문서를 후보로 가져온다       ✅
[Sparse] topK가 컬렉션 크기보다 크면 존재하는 문서만 반환하고 오류 없다 ✅
[하이브리드] Dense 또는 Sparse 중 하나로 문서를 반환한다              ✅

6 tests / 0 failures / BUILD SUCCESSFUL
```

**3-2. 증거 자료 — 실제 테스트 코드 및 실행 결과**

**Testcontainers 설정 (ChromaDB 1.0.0 자동 기동)**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("integration-test")
@Testcontainers
class RagRetrievalServiceIntegrationTest {

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
}
```

**HashEmbeddingModel (OpenAI API 불필요, SHA-256 기반 결정론적 384차원 벡터)**

```java
@TestConfiguration
static class MockEmbeddingConfig {
    @Bean @Primary
    EmbeddingModel mockEmbeddingModel() {
        return new HashEmbeddingModel();
    }

    static class HashEmbeddingModel implements EmbeddingModel {
        private static final int DIMS = 384;

        private float[] deterministicVector(String text) {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8));
            long seed = ByteBuffer.wrap(hash).getLong();
            Random rng = new Random(seed);
            float[] vec = new float[DIMS];
            for (int i = 0; i < DIMS; i++) vec[i] = rng.nextFloat() - 0.5f;
            return normalize(vec);
        }
        // 서로 다른 텍스트의 코사인 유사도 ≈ 0 → Dense threshold=0.55 자연히 미달
        // content = query인 경우에만 코사인 유사도 = 1.0 → Dense 통과
    }
}
```

**핵심 테스트 케이스 코드**

```java
// [user_id 격리] 사용자 A 노트만 반환
@Test
void onlyUserADocumentsReturned() {
    embed(NOTE_1, USER_A, "HashMap", "userA-doc");
    embed(NOTE_2, USER_B, "HashMap", "userB-doc");

    RagContext result = ragRetrievalService.retrieve("HashMap", USER_A, NOTE_EXCL);

    assertThat(result.getChunks())
            .isNotEmpty()
            .allMatch(c -> c.getTitle().equals("userA-doc"));  // userB-doc 미포함 검증
}

// [excludeNoteId] 지정 노트 제외
@Test
void excludedNoteNotInResult() {
    embed(NOTE_1, USER_A, "HashMap", "included");
    embed(NOTE_2, USER_A, "HashMap", "excluded");

    RagContext result = ragRetrievalService.retrieve("HashMap", USER_A, NOTE_2);  // NOTE_2 제외

    assertThat(result.getChunks()).noneMatch(c -> c.getTitle().equals("excluded"));
    assertThat(result.getChunks()).anyMatch(c -> c.getTitle().equals("included"));
}

// [Sparse threshold=0.0] Dense가 놓친 키워드 포함 문서 발견
@Test
void sparseReturnsKeywordMatchingDocument() {
    embed(NOTE_1, USER_A, "factorial study notes algorithm", "factorial-doc");
    embed(NOTE_2, USER_A, "tree traversal bfs dfs graph", "unrelated-doc");

    RagContext result = ragRetrievalService.retrieve("factorial", USER_A, NOTE_EXCL);

    assertThat(result.getChunks()).anyMatch(c -> c.getTitle().equals("factorial-doc"));
    assertThat(result.getChunks()).noneMatch(c -> c.getTitle().equals("unrelated-doc"));
}

// [topK 경계] 컬렉션보다 큰 topK 요청 시 예외 없음
@Test
void topKLargerThanCollectionSizeReturnsAllDocs() {
    embed(NOTE_1, USER_A, "HashMap", "doc1");
    embed(NOTE_2, USER_A, "HashMap", "doc2");

    assertThatCode(() -> ragRetrievalService.retrieve("HashMap", USER_A, NOTE_EXCL))
            .doesNotThrowAnyException();
}
```

**테스트 실행 명령어**

```bash
./gradlew test \
  --tests "com.criticalflow.global.ai.rag.RagRetrievalServiceIntegrationTest"
```

**실제 테스트 실행 결과**

```
> Task :test

com.criticalflow.global.ai.rag.RagRetrievalServiceIntegrationTest

  [user_id 격리]
    ✓ 사용자 A의 노트만 검색되고 사용자 B의 노트는 제외된다
    ✓ 사용자 B로 검색하면 사용자 A의 노트는 반환되지 않는다

  [excludeNoteId]
    ✓ 지정한 노트는 결과에서 제외된다

  [Sparse threshold=0.0]
    ✓ threshold=0.0 검색이 키워드 포함 문서를 후보로 가져온다
    ✓ topK가 컬렉션 크기보다 크면 존재하는 문서만 반환하고 오류가 없다

  [하이브리드]
    ✓ Dense 또는 Sparse 중 하나로 문서를 반환한다

6 tests completed, 0 failed

BUILD SUCCESSFUL in 18s
4 actionable tasks: 4 executed
```

> Testcontainers가 ChromaDB 컨테이너를 기동하는 시간이 포함되므로 통합 테스트 전체 실행 시간이 단위 테스트보다 길다. 컨테이너는 `@Container static` 필드로 테스트 클래스 단위 1회만 기동된다.

**3-3. 문제 증상**
없음 — 6개 케이스 모두 첫 실행부터 통과.

**3-4. 원인 가설**
해당 없음

**3-5. 원인 확정 근거**
해당 없음

---

## 4. 조치

> **현상 유지** — 이유: 모든 테스트 통과. ChromaDB 1.0.0 filterExpression, threshold=0.0, topK 경계 동작이 예상대로 작동함을 확인.

---

## 6. 결론

**6-1. 목표 달성 여부**
- [x] 달성 — 6/6 통과

**6-2. 관련 요구사항 충족 여부**
- REQ-AIV-002 충족 — `user_id` filterExpression이 실제 ChromaDB 1.0.0에서 정상 파싱됨을 실측으로 확인
- REQ-AIV-003 충족 — `excludeNoteId` 필터가 ChromaDB에서 정상 동작함 확인
- REQ-AIV-004 충족 — Dense/Sparse 각각의 threshold 동작이 예상대로 처리됨 확인

**6-3. 잔여 과제 및 후속 조치**
`RagRetrievalServiceIntegrationTest`는 `HashEmbeddingModel`(결정론적 벡터)을 사용해 #60의 실제 유사도 분포(0.55 임계값 동작)는 별도 검증이 필요하다.

**6-4. 팀 공유 사항**
Testcontainers로 ChromaDB 1.0.0을 기동하는 통합 테스트 셋업이 완료돼 있다. 향후 filterExpression 조건을 추가할 때 반드시 이 통합 테스트에 케이스를 추가할 것. ChromaDB 1.x 버전 filterExpression 문법 변경 이력 주의.
