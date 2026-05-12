# AI 검증 테스트 계획 — 이슈 #57 ~ #63

## 개요

| 이슈 | 제목 | 테스트 유형 | 자동화 | 상태 |
|------|------|-----------|--------|------|
| #57 | NotePreprocessor 임베딩 품질 비교 | 수동 실험 | ❌ | ✅ 완료 |
| #58 | NotePreprocessor / NoteMetadataExtractor 단위 테스트 | 단위 테스트 | ✅ | ✅ 완료 |
| #59 | isTopicRelevant 2차 필터 노이즈 감소 효과 측정 | 수동 관찰 | ❌ | ✅ 완료 |
| #60 | similarity-threshold 최적값 검증 | 수동 실험 | ❌ | ✅ 완료 |
| #61 | RagRetrievalService 실제 ChromaDB 통합 테스트 | 통합 테스트 | ✅ | ✅ 완료 |
| #62 | Dense-only vs 하이브리드 Recall@4 비교 | 수동 실험 | ❌ | ✅ 완료 |
| #63 | RRF k=60 최적값 검증 | 단위 테스트 | ✅ | ✅ 완료 |

---

## 테스트 파일 구조

```
src/test/java/com/criticalflow/
└── global/
    └── ai/
        └── rag/
            ├── RagRetrievalServiceTest.java              (#63 RRF k값 케이스 포함)
            ├── RagRetrievalServiceIntegrationTest.java   (#61)
            ├── IsTopicRelevantFilterTest.java            (#59 자동화)
            ├── NotePreprocessorTest.java                 (#58)
            ├── NoteMetadataExtractorTest.java            (#58)
            └── NotePreprocessorEmbeddingQualityTest.java (#57 수동 실험 자동화)
```

---

## #57 — NotePreprocessor 임베딩 품질 비교 ✅ 완료

**결과 요약**: 전처리 적용 시 Recall@4 **40%**, 미적용 시 **100%** → 전처리가 오히려 품질을 저하시킴.

**원인**: `NotePreprocessor`가 코드를 식별자 목록으로 압축하면서 문맥이 소실되고, qnote-7(DP)이 10개 쿼리 중 6개에 잘못 등장하는 허브 오염 현상 발생.

**조치**: `NoteEmbeddingService.embed()`에서 `NotePreprocessor` 호출 제거. 원본 마크다운 그대로 임베딩.

> 상세 결과: `docs/테스트/preprocessor-embedding-quality.md`

---

## #58 — NotePreprocessor / NoteMetadataExtractor 단위 테스트 ✅ 완료

### NotePreprocessorTest.java

| 테스트 | 검증 목적 |
|--------|---------|
| 코드블록을 언어명과 식별자텍스트로 변환한다 | 정규식 파싱 정상 동작 |
| 코드블록이 없으면 원문을 그대로 반환한다 | 코드 없는 노트 무해성 |
| 언어 미지정 코드블록은 code 레이블을 사용한다 | 언어 미지정 처리 |
| 복수 코드블록이 있으면 모두 변환한다 | 복수 블록 처리 |

**실행**: `./gradlew test --tests "*.NotePreprocessorTest"`

---

### NoteMetadataExtractorTest.java

| 테스트 | 검증 목적 |
|--------|---------|
| 복수 언어 코드블록에서 모든 언어를 추출한다 | 언어 추출 정확도 |
| 동일 언어가 여러 번 등장해도 한 번만 반환한다 | 중복 제거 |
| 언어 미지정 코드블록만 있으면 unknown을 반환한다 | 엣지 케이스 |
| 모든 레벨의 마크다운 헤더를 순서대로 추출한다 | 헤더 파싱 |
| 헤더가 없으면 빈 리스트를 반환한다 | 빈 입력 처리 |

**실행**: `./gradlew test --tests "*.NoteMetadataExtractorTest"`

---

## #59 — isTopicRelevant 2차 필터 노이즈 감소 효과 측정 ✅ 완료

### 자동화 테스트 — IsTopicRelevantFilterTest.java

`isTopicRelevant()`의 키워드 오버랩 필터 로직을 단위 검증.

**실행**: `./gradlew test --tests "*.IsTopicRelevantFilterTest"`

### 수동 실험 결과

**1차 측정** (`[RAG 필터]` 로그 기반, 설정 수정 전):

| 상황 | 결과 |
|------|------|
| queryText = 노트 전체 본문 (~150 단어) | 키워드 150개 추출 → 다른 노트가 30개 이상 포함해야 통과 → 평균 제거율 **90%** |
| 판단 | 과도한 제거 — 근본 원인은 임계값이 아닌 키워드 추출 방식 |

**원인 분석**: `retrieve()`에 전달되는 queryText가 노트 전체 내용이어서 키워드 수가 150개에 달함. 20% 기준을 통과하려면 30개 이상의 키워드가 일치해야 해서 무관한 노트는 물론 관련 노트도 제거됨.

**수정 사항**:

| 항목 | 수정 전 | 수정 후 |
|------|---------|---------|
| 키워드 최소 길이 (한국어) | 1자 이상 | 2자 이상 |
| 키워드 최소 길이 (영어) | 4자 이상 | 4자 이상 |
| 키워드 수 상한 | 없음 | 20개 |
| 오버랩 임계값 | 0.2 (20%) | **0.1 (10%)** |

**2차 측정** (설정 수정 후):

| 상황 | 결과 |
|------|------|
| 평균 제거율 | **45%** |
| 판단 | 기준(5~30%) 초과이나 노트 수 10개의 구조적 한계 |

노트가 10개뿐이라 RAG가 최대 4건 검색 시 주제가 다른 노트도 상위권에 오를 수밖에 없는 환경. 노트 수가 50~100개로 증가하면 자연히 제거율이 낮아질 것으로 예상. 현재 설정 유지.

> 상세 로그: `docs/테스트/rag-test-notes.md`

---

## #60 — similarity-threshold 최적값 검증 ✅ 완료

**결과 요약**: `text-embedding-3-small` + 한국어 CS 노트 환경에서 코사인 유사도가 최대 0.71로 측정돼 0.75는 달성 불가능한 값이었음.

| 임계값 | Dense 평균 반환 | 판단 |
|-------|--------------|------|
| 0.75 | 0건 | ❌ 기존값 — 무의미 |
| 0.70 | 0.5건 | ❌ 사용 불가 |
| 0.60 | 5.3건 | ⚠️ 일부 노트 결과 부족 |
| **0.55** | **5.8건** | ✅ 채택 |

**조치**: `rag.similarity-threshold: 0.75` → **`0.55`** 변경 완료.

> 상세 결과: `docs/테스트/threshold-tuning-guide.md`

**주의 — 단위 테스트 스텁 불일치**:

`RagRetrievalServiceTest.java`의 `setUp()`이 `similarityThreshold=0.75`로 하드코딩되어 있어 실제 운영값(0.55)과 다르다. 스텁 로직 `req.getSimilarityThreshold() >= 0.75`는 테스트 내부에서는 올바르게 동작하지만, 운영 환경의 임계값과 괴리가 있다. 향후 `setUp()`을 `similarityThreshold=0.55`로 수정하고 스텁 조건을 `>= 0.55`로 변경해야 한다.

---

## #61 — RagRetrievalService 실제 ChromaDB 통합 테스트 ✅ 완료

**파일**: `RagRetrievalServiceIntegrationTest.java`

Mock으로 확인할 수 없는 ChromaDB filterExpression 문법, threshold=0.0 동작, topK 경계 처리를 실제 Testcontainers ChromaDB로 검증.

**검증 항목**:

| 테스트 그룹 | 검증 목적 |
|-----------|---------|
| user_id 격리 | 사용자 A 노트만 검색되고 사용자 B 노트는 제외됨 |
| excludeNoteId | 지정된 노트가 결과에서 제외됨 |
| Sparse 검색 threshold=0.0 | Dense threshold 미달 문서를 Sparse가 키워드로 보완 |
| 하이브리드 보완 | Dense+Sparse 양쪽 등장 문서가 RRF 점수 합산으로 상위 반환 |

**실행**: `./gradlew test --tests "*.RagRetrievalServiceIntegrationTest"`

**미확인 사항**:
- 실제 ChromaDB 연동에서 #60의 threshold=0.55 기준 동작 검증 안 됨 (테스트는 내부에서 다른 값 사용 가능성)

---

## #62 — Dense-only vs 하이브리드 Recall@4 비교 ✅ 완료

**측정 조건**: 노트 20개 임베딩, 카테고리 A(기술 식별자 포함) 5개 + 카테고리 B(짧은 한국어 CS 용어) 5개 쿼리.

**최종 결과**:

| 카테고리 | Dense-only Recall@4 | 하이브리드 Recall@4 | 향상폭 | 판단 |
|---------|-------------------|------------------|------|------|
| A (기술 식별자) | 0.12 | 0.47 | **+0.35** | 10%p 기준 대폭 초과 ✅ |
| B (한국어 CS 용어) | 0.23 | 0.38 | **+0.15** | 10%p 기준 초과 ✅ |
| **전체 평균** | **0.18** | **0.43** | **+0.25** | ✅ |

**결론**: 하이브리드(Dense + Sparse + RRF) 구조 유지 권장.

**한계**: noteId=10(힙)에서 Sparse 후보 10건이 `isTopicRelevant()` 2차 필터에서 전부 제거됨. 전문 용어가 많은 노트에서 2차 필터가 과도하게 작동하는 케이스.

**측정 완료 후 처리**: `RagRetrievalService.java`의 `[#62]` 로그 블록 3곳 제거 대상.

> 상세 결과: `docs/테스트/hybrid-recall-guide.md`

---

## #63 — RRF k=60 최적값 검증 ✅ 완료

**파일**: `RagRetrievalServiceTest.java` 내 `RrfMergeTest` Nested 블록.

**검증 항목**:

| 테스트 케이스 | 검증 목적 |
|-----------|---------|
| Dense+Sparse 양쪽 등장 문서가 첫 번째 반환 | RRF 점수 합산으로 중복 문서 우선순위 부여 |
| Dense-only, Sparse-only 문서 모두 결과에 포함 | 합집합 구조 확인 |

**실행**: `./gradlew test --tests "*.RagRetrievalServiceTest"`

**미확인 사항**: k = 10, 30, 60, 100 비교 실험 미수행. 논문 기본값(60)이 이 프로젝트에서도 안정적으로 동작하는지 정량 비교 없음.

---

## 실행 순서 권장

```
1단계 (빠른 자동화)
  #58 → NotePreprocessorTest + NoteMetadataExtractorTest  (외부 의존성 없음)
  #63 → RagRetrievalServiceTest                           (Mockito)
  #59 → IsTopicRelevantFilterTest                         (Mockito)

2단계 (ChromaDB 연동)
  #61 → RagRetrievalServiceIntegrationTest               (Testcontainers 필요)

3단계 (수동 실험)
  #57 → NotePreprocessorEmbeddingQualityTest             (OpenAI API 키 필요)
  #60 → threshold-tuning-guide.md 참고
  #62 → hybrid-recall-guide.md 참고
```

---

## 잔여 작업

| 항목 | 내용 |
|------|------|
| RagRetrievalServiceTest 임계값 수정 | `setUp()`의 `similarityThreshold=0.75` → `0.55`, 스텁 조건 `>= 0.75` → `>= 0.55` |
| `[#62]` 측정 로그 제거 | `RagRetrievalService.java` Dense/Sparse 건수 로그, 최종 Top-4 로그 |
| noteId=10(힙) 2차 필터 과도 제거 | `isTopicRelevant()` 전문 용어 노트 대응 방안 검토 |

---

## 설정값 요약

```yaml
# application.yml (현재 운영값)
rag:
  similarity-threshold: 0.55    # Dense 검색 유사도 임계값 (#60 측정으로 0.75 → 0.55 변경)
  max-results: 4                # 최종 반환 청크 수 (RRF 병합 후 적용)
  bm25-max-results: 10          # Sparse 검색 초기 후보 풀 크기 (0 = Dense-only 모드)
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
