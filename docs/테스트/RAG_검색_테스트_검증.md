
## 4. 의도대로 작동하는지 확인한 방법과 결과

### 4.1 NoteEmbeddingService

**확인 방법**: 수동 API 호출 후 ChromaDB HTTP API로 벡터 존재 여부 직접 조회

| 검증 시나리오 | 방법 | 결과 |
|-------------|------|------|
| 임베딩 저장 | `POST /api/notes`로 노트 생성 → ChromaDB `GET /api/v1/collections/{id}/get` 조회 | 문서 ID `note-{noteId}`로 저장 확인 |
| 수정 시 재임베딩 | `PUT /api/notes/{id}`로 수정 → ChromaDB 재조회 | 기존 벡터 삭제 후 새 벡터로 교체 확인 |
| 삭제 시 벡터 제거 | `DELETE /api/notes/{id}` → ChromaDB 재조회 | 문서 제거 확인 |

**미확인 사항**: `NotePreprocessor` 전처리가 실제로 임베딩 품질을 향상시키는지 수치로 검증하지 않았다. 전처리 적용/미적용 두 조건에서 유사도 점수 분포를 비교한 실험은 수행하지 않았다.

---

### 4.2 NotePreprocessor / NoteMetadataExtractor

**확인 방법**: 단위 테스트 미작성. 로컬에서 수동으로 입력/출력 결과를 눈으로 확인하는 수준에서 검증했다.

| 검증 항목 | 결과 |  
|---------|------|
| 코드 블록이 `[python: ...]` 형태로 변환 | 육안 확인 |
| 코드 없는 노트는 원문 그대로 유지 | 육안 확인 |
| 복수 언어 코드 블록에서 언어 목록 추출 | 육안 확인 |
| 마크다운 헤더 목록 정확히 추출 | 육안 확인 |

**미확인 사항**: 자동화된 단위 테스트가 없어, 로직 변경 시 회귀 여부를 즉시 감지할 수 없다.

---

### 4.3 RagRetrievalService — 기존 Dense-only

**확인 방법**: 대화 시작 API 호출 후 서버 로그로 RAG 반환 결과 관찰

| 검증 시나리오 | 테스트 내용 | 결과 |
|-------------|-----------|------|
| 유사 노트 존재 | "재귀" 노트 저장 후 "스택 오버플로우" 주제로 대화 시작 | "재귀" 노트가 RAG 결과에 포함 |
| 유사 노트 없음 | 완전히 다른 주제로 대화 시작 | `(No relevant past notes found.)` 반환 |
| 자기 참조 방지 | 현재 노트 자체가 결과에 포함되는지 확인 | `excludeNoteId` 필터로 제외 확인 |
| 다른 사용자 노트 격리 | 사용자 B 노트가 사용자 A 검색에서 나오는지 확인 | `user_id` 필터로 차단 확인 |
| 한국어 키워드 필터 버그 | 1자 한국어 키워드가 2차 필터를 통과하는지 확인 | `>` 연산자로 인해 1자 키워드 제외되는 버그 발견 → `>=`로 수정 (commit `29f6bd9`) |

**미확인 사항**:
- 2차 필터(키워드 오버랩 20%)가 실제 노이즈를 얼마나 줄이는지 정량 측정하지 않음
- `similarity-threshold: 0.75`가 최적값인지 다른 임계값(0.6, 0.8)과 비교하지 않음

---

### 4.4 RagRetrievalService — 하이브리드 검색

**확인 방법**: Mockito 기반 단위 테스트 작성 (`RagRetrievalServiceTest.java`)

VectorStore를 Mock으로 교체하고, `SearchRequest.getSimilarityThreshold() >= 0.75` 여부로 Dense/Sparse 호출을 구분하는 스텁 방식을 사용했다.

```java
private void stubSearch(List<Document> denseResult, List<Document> sparseResult) {
    when(vectorStore.similaritySearch(any(SearchRequest.class))).thenAnswer(inv -> {
        SearchRequest req = inv.getArgument(0);
        return req.getSimilarityThreshold() >= 0.75 ? denseResult : sparseResult;
    });
}
```

**테스트 결과 (전체 통과)**:

| 테스트 그룹 | 테스트 케이스 | 검증 목적 | 결과 |
|-----------|-----------|---------|------|
| `RrfMergeTest` | Dense+Sparse 양쪽 등장 문서가 첫 번째 반환 | RRF 점수 합산으로 중복 문서 우선순위 부여 | 통과 |
| `RrfMergeTest` | Dense-only, Sparse-only 문서 모두 결과에 포함 | 합집합 구조 확인 | 통과 |
| `SparseSearchTest` | Dense threshold 미달 코드 식별자 문서를 Sparse가 발견 | Dense가 놓친 문서 보완 역할 | 통과 |
| `SparseSearchTest` | 키워드 미포함 문서는 Sparse 결과에서 제외 | 키워드 필터 동작 확인 | 통과 |
| `KoreanKeywordTest` | 2자 한국어 키워드("스택") 포함 문서 검색 | minLength=1 정상 동작 | 통과 |
| `KoreanKeywordTest` | 1자 한국어 CS 용어("큐") 포함 문서 검색 | 버그 수정(2→1) 후 1자 용어 인식 | 통과 |
| `TopicRelevantFilterTest` | 오버랩 20% 미만 문서 최종 결과에서 제외 | RRF 이후 2차 필터 동작 | 통과 |
| `TopicRelevantFilterTest` | 오버랩 20% 이상 문서 결과에 포함 | 관련 문서 통과 확인 | 통과 |
| (독립) | Dense+Sparse 모두 빈 결과 시 빈 RagContext 반환 | `isEmpty()` 및 포맷 메시지 | 통과 |
| (독립) | 결과 4개 초과 시 maxResults로 제한 | 반환 청크 수 상한 확인 | 통과 |

**미확인 사항**:
- 실제 ChromaDB 연동 검증 없음 — VectorStore를 Mock으로 교체했기 때문에 실제 DB와의 연동 동작 보장 불가
- Dense-only 대비 하이브리드의 Recall 향상 수치를 측정하지 않음
- RRF `k=60`이 이 프로젝트에 최적인지 검증하지 않음 (논문 기본값 그대로 사용)

---

### 4.5 RagContext

**확인 방법**: `AiTutorService.generateFirstQuestion()` 호출 시 System Prompt 전체를 로그로 출력해 포맷 확인

| 검증 항목 | 결과 |
|---------|------|
| 검색 결과 있을 때 레이블 + 청크 목록 포맷 | 정상 출력 확인 |
| 검색 결과 없을 때 기본 메시지 | `(No relevant past notes found.)` 확인 |
| similarity 점수 포함 여부 | `similarity: 0.83` 형태로 포함 확인 |
| 레이블 추가 효과 | 추가 전후로 AI가 RAG 결과를 현재 노트로 오해하는 빈도 감소 수동 확인 (정량 데이터 없음) |

---

### 4.6 FocusEventFormatter

**확인 방법**: 집중 이탈 이벤트를 DB에 직접 INSERT한 후 대화 시작 API 호출, 서버 로그로 포맷된 문자열 확인

| 검증 시나리오 | 결과 |
|-------------|------|
| 최근 15분 내 이벤트 있음 | `[GAZE_OUT | 8s | alerted=false]` 형식 포맷 확인 |
| 이벤트 없음 | `(No focus events in the last 15 minutes.)` 반환 확인 |
| `lookback-minutes` 경계 밖 이벤트 | 조회 범위 밖 이벤트 미포함 확인 |

**미확인 사항**: AI가 집중 이벤트 데이터를 실제로 활용해 질문 방향을 바꾸는지 일관되게 검증하지 않았다.

---

## 5. 향후 테스트 계획

현재 자동화된 테스트가 없거나 수동 확인에 그친 항목들이다.

---

### 5.1 NotePreprocessor 단위 테스트

**목표**: 코드 블록 변환이 의도한 형식으로 출력되는지 검증

```java
@ExtendWith(MockitoExtension.class)
class NotePreprocessorTest {

    NotePreprocessor preprocessor = new NotePreprocessor();

    @Test
    @DisplayName("코드 블록을 언어명과 식별자 텍스트로 변환한다")
    void preprocessForEmbedding_코드블록_식별자텍스트로변환() {
        String input = "개요\n```python\ndef fibonacci(n):\n    return n\n```\n설명";
        String result = preprocessor.preprocessForEmbedding(input);

        assertThat(result).contains("[python:");
        assertThat(result).contains("fibonacci").contains("return");
        assertThat(result).contains("개요").contains("설명");
        assertThat(result).doesNotContain("def ");
    }

    @Test
    @DisplayName("코드 블록이 없으면 원문을 그대로 반환한다")
    void preprocessForEmbedding_코드블록없으면_원문반환() {
        String input = "순수 텍스트 노트입니다.";
        assertThat(preprocessor.preprocessForEmbedding(input)).isEqualTo(input);
    }

    @Test
    @DisplayName("언어 미지정 코드 블록은 [code: ...] 레이블을 사용한다")
    void preprocessForEmbedding_언어미지정_code레이블사용() {
        String input = "```\nsome code here\n```";
        assertThat(preprocessor.preprocessForEmbedding(input)).startsWith("[code:");
    }
}
```

---

### 5.2 NoteMetadataExtractor 단위 테스트

**목표**: 언어 목록 추출과 헤더 목록 추출이 정확한지 검증

```java
class NoteMetadataExtractorTest {

    NoteMetadataExtractor extractor = new NoteMetadataExtractor();

    @Test
    @DisplayName("복수 언어 코드 블록에서 모든 언어를 추출한다")
    void extractLanguages_복수언어_모두추출() {
        String markdown = "```python\ncode\n```\n```java\ncode\n```";
        assertThat(extractor.extractLanguages(markdown))
            .containsExactlyInAnyOrder("python", "java");
    }

    @Test
    @DisplayName("언어 미지정 코드 블록만 있으면 unknown을 반환한다")
    void extractLanguages_언어미지정_unknown반환() {
        assertThat(extractor.extractLanguages("```\nsome code\n```"))
            .containsExactly("unknown");
    }

    @Test
    @DisplayName("모든 레벨의 마크다운 헤더를 순서대로 추출한다")
    void extractHeaders_마크다운헤더_추출() {
        String markdown = "# 제목\n## 소제목\n내용\n### 세부 항목";
        assertThat(extractor.extractHeaders(markdown))
            .containsExactly("제목", "소제목", "세부 항목");
    }
}
```

---

### 5.3 RagRetrievalService — 기존 Dense-only 2차 필터 단위 테스트

**목표**: `isTopicRelevant()`가 한국어/영어 기준에 맞게 동작하는지 검증  
`private` 메서드이므로 `retrieve()` 전체 흐름으로 간접 검증하거나 패키지 가시성 변경 적용

```java
@Test
@DisplayName("키워드 오버랩 20% 이상이면 2차 필터를 통과한다")
void isTopicRelevant_영어키워드_20퍼센트이상겹치면통과() {
    // 쿼리: "java spring boot" (3개 키워드)
    // 문서: "spring framework boot application"
    // → "spring"(✓), "boot"(✓) — 2/3 = 66% → 통과
}

@Test
@DisplayName("한국어 쿼리에서 최소 길이 1자 기준이 적용된다")
void isTopicRelevant_한국어키워드_1자기준적용() {
    // 쿼리: "큐 자료구조"
    // "큐"(1자)도 meaningful keyword로 인정되어 필터에 포함
}

@Test
@DisplayName("의미 있는 키워드가 없으면 항상 통과한다")
void isTopicRelevant_의미없는단어만있으면통과() {
    // 쿼리: "a b c" (영어 3자 미만 → significant = 0)
    // significant == 0 → true 반환
}
```

---

### 5.4 ChromaDB 통합 테스트 (기존 + 하이브리드)

**목표**: 실제 ChromaDB와 연동해 end-to-end 검색 흐름 검증

```
사전 조건 (Testcontainers로 ChromaDB Docker 컨테이너 실행):
  - 사용자 A 노트 3개 임베딩: "HashMap 정리", "재귀 알고리즘", "스프링 부트 설정"
  - 사용자 B 노트 1개 임베딩: "HashMap 사용법"

검증 항목:
  [기존 Dense-only]
  1. 사용자 A로 "HashMap" 검색 → 사용자 A 노트만 포함, 사용자 B 노트 미포함
  2. excludeNoteId 지정 시 해당 노트 미포함
  3. 유사도 0.75 이하 문서 미포함

  [하이브리드 추가]
  4. Dense threshold 미달이지만 정확한 키워드 포함 노트가 Sparse로 보완
  5. 양쪽에 등장한 문서가 RRF 점수 합산으로 첫 번째 반환
  6. threshold=0.0 Sparse 검색이 의도한 후보 수(topK=10)를 반환하는지 확인
```

---

### 5.5 FocusEventFormatter 단위 테스트

**목표**: 이벤트 포맷팅과 시간 범위 필터링이 정확한지 검증

```java
@ExtendWith(MockitoExtension.class)
class FocusEventFormatterTest {

    @Mock FocusEventRepository repository;
    @InjectMocks FocusEventFormatter formatter;

    @Test
    @DisplayName("이벤트 목록을 포맷된 문자열로 반환한다")
    void format_이벤트있으면포맷된문자열반환() {
        FocusEvent event = /* GAZE_OUT, 8초, alerted=false */;
        when(repository.findBySessionIdAndDetectedAtAfterOrderByDetectedAtAsc(any(), any()))
            .thenReturn(List.of(event));

        String result = formatter.format(1L);
        assertThat(result).isEqualTo("[GAZE_OUT | 8s | alerted=false]");
    }

    @Test
    @DisplayName("이벤트가 없으면 기본 메시지를 반환한다")
    void format_이벤트없으면기본메시지반환() {
        when(repository.findBySessionIdAndDetectedAtAfterOrderByDetectedAtAsc(any(), any()))
            .thenReturn(List.of());

        assertThat(formatter.format(1L))
            .contains("No focus events in the last");
    }
}
```

---

### 5.6 Dense vs 하이브리드 Recall@4 비교 실험 (수동)

**목표**: 하이브리드 검색이 Dense-only 대비 실제로 더 많은 관련 노트를 찾는지 정량 측정

```
실험 절차:
  1. 코드 식별자 포함 노트 10개, 한국어 CS 용어 노트 10개 준비 (총 20개)
  2. 각 노트를 ChromaDB에 임베딩
  3. 각 노트에 대응하는 검색 쿼리 10개 작성
  4. Dense-only / 하이브리드 두 조건으로 retrieve() 실행
  5. 정답 노트(ground truth)가 Top-4 결과에 포함됐는지로 Recall@4 계산
  6. 두 조건의 Recall@4 비교

기대 결과:
  - 코드 식별자/짧은 한국어 CS 용어 쿼리 → 하이브리드 Recall@4 > Dense-only
  - 긴 설명형 쿼리 → 두 조건 결과 유사
```

---

### 5.7 임베딩 품질 비교 실험 — NotePreprocessor 효과 측정 (수동)

**목표**: `NotePreprocessor` 전처리 적용 여부에 따른 검색 품질 차이 정량 측정

```
실험 절차:
  1. 코드 블록 포함 노트 10개 준비
  2. 전처리 적용 / 미적용 두 조건으로 각각 ChromaDB에 임베딩
  3. 동일한 쿼리 5개로 유사도 검색
  4. Top-4 결과의 관련성을 1-5점으로 평가
  5. 두 조건의 평균 관련성 점수 비교

기대 결과:
  - 전처리 적용 시 코드 관련 노트의 유사도 점수 분포가 더 의미 있게 분리됨
```

---

### 5.8 RRF k 값 민감도 분석 (선택)

**목표**: `k=60`(논문 기본값)이 이 프로젝트에 최적인지 확인

```
분석 방법:
  1. 동일한 Dense/Sparse 결과 리스트를 고정
  2. k = 10, 30, 60, 100 각각으로 mergeWithRRF() 실행
  3. 최종 Top-4 순위 변화 비교
  4. 순위 변화가 유의미하다면 Recall@4 실험으로 최적값 결정
```

---

## 6. 설정값 요약

```yaml
# application.yml
rag:
  similarity-threshold: 0.75    # Dense 검색 유사도 임계값 (Sparse는 항상 0.0)
  max-results: 4                # 최종 반환 청크 수 (RRF 병합 후 적용)
  bm25-max-results: 10          # Sparse 검색 초기 후보 풀 크기
  focus-lookback-minutes: 15    # 집중 이탈 이벤트 조회 범위 (분)
```

| 설정값 | 영향 범위 | 조정 기준 |
|--------|---------|---------|
| `similarity-threshold` | Dense 검색 1차 필터 | 낮추면 관련성 낮은 문서 포함 위험, 높이면 검색 누락 증가 |
| `max-results` | RRF 병합 후 최종 반환 수 | 많을수록 AI에게 더 많은 컨텍스트, System Prompt 길이 증가 |
| `bm25-max-results` | Sparse 검색 후보 수 | 크게 잡을수록 키워드 히트 확률 높아지나 불필요한 후보도 증가 |
| `focus-lookback-minutes` | FocusEventFormatter 조회 범위 | 짧으면 최근 이벤트만, 길면 오래된 이벤트도 포함 |

**인프라 정보**:
- ChromaDB 컬렉션: `criticalflow-notes`
- 임베딩 모델: `text-embedding-3-small` (OpenAI)
- LLM: `gpt-4o` (질문 생성 + 질문 타입 분류)
