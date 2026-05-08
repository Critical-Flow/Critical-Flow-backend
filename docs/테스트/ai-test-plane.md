# AI 검증 테스트 계획 — 이슈 #57 ~ #63

## 개요

| 이슈 | 제목 | 테스트 유형 | 자동화 |
|------|------|-----------|--------|
| #57 | NotePreprocessor 임베딩 품질 비교 | 수동 실험 | ❌ |
| #58 | NotePreprocessor / NoteMetadataExtractor 단위 테스트 | 단위 테스트 | ✅ |
| #59 | isTopicRelevant 2차 필터 노이즈 감소 효과 측정 | 수동 관찰 | ❌ |
| #60 | similarity-threshold 0.75 최적값 검증 | 수동 실험 | ❌ |
| #61 | RagRetrievalService 실제 ChromaDB 통합 테스트 | 통합 테스트 | ✅ |
| #62 | Dense-only vs 하이브리드 Recall@4 비교 | 수동 실험 | ❌ |
| #63 | RRF k=60 최적값 검증 | 단위 테스트 | ✅ |

---

## 테스트 파일 구조

```
src/test/java/com/criticalflow/
└── global/
    └── ai/
        └── rag/
            ├── RagRetrievalServiceTest.java              (기존 — #63 RRF k값 케이스 추가)
            ├── RagRetrievalServiceIntegrationTest.java   (#61 신규)
            ├── NotePreprocessorTest.java                 (#58 신규)
            └── NoteMetadataExtractorTest.java            (#58 신규)
```

---

## 자동화 테스트 (#58, #61, #63)

### #58 — NotePreprocessorTest.java

**목적**: 외부 의존성 없는 순수 Java 로직. 정규식 파싱 결과를 입력/출력으로 직접 검증.

```
NotePreprocessorTest
├── [코드블록 변환]
│   ├── 코드블록을_언어명과_식별자텍스트로_변환한다
│   ├── 코드블록이_없으면_원문을_그대로_반환한다
│   ├── 언어_미지정_코드블록은_code_레이블을_사용한다
│   └── 복수_코드블록이_있으면_모두_변환한다
├── [식별자 필터링]
│   ├── 3자_이하_토큰은_식별자에서_제외된다
│   └── 구문_키워드(def_return_등)는_제거된다
└── [본문 보존]
    └── 코드블록_외_일반_텍스트는_변환_후에도_유지된다
```

**실행**: `./gradlew test --tests "*.NotePreprocessorTest"`

---

### #58 — NoteMetadataExtractorTest.java

**목적**: `extractLanguages()` / `extractHeaders()` 의 엣지 케이스 커버.

```
NoteMetadataExtractorTest
├── [언어 추출]
│   ├── 복수_언어_코드블록에서_모든_언어를_추출한다
│   ├── 동일_언어가_여러_번_등장해도_한_번만_반환한다
│   └── 언어_미지정_코드블록만_있으면_unknown을_반환한다
└── [헤더 추출]
    ├── 모든_레벨의_마크다운_헤더를_순서대로_추출한다
    ├── 헤더가_없으면_빈_리스트를_반환한다
    └── 코드블록_내부의_샵_기호는_헤더로_인식하지_않는다
```

**실행**: `./gradlew test --tests "*.NoteMetadataExtractorTest"`

---

### #61 — RagRetrievalServiceIntegrationTest.java

**목적**: Mock으로 확인할 수 없는 ChromaDB filterExpression 문법, threshold=0.0 동작, topK 경계 처리를 실제 컨테이너로 검증.

**사전 준비**: `build.gradle`에 의존성 추가
```groovy
testImplementation 'org.testcontainers:testcontainers:1.19.8'
testImplementation 'org.testcontainers:junit-jupiter:1.19.8'
```

```
RagRetrievalServiceIntegrationTest  (@SpringBootTest + @Testcontainers)
├── [user_id 격리]
│   ├── 사용자A_노트만_검색되고_사용자B_노트는_제외된다
│   └── userId_필터_없이는_타_사용자_노트가_반환될_수_있다  (네거티브)
├── [excludeNoteId]
│   └── excludeNoteId_지정시_해당_노트가_결과에서_제외된다
├── [Sparse 검색 — threshold=0.0]
│   ├── threshold_0_0_Sparse검색이_키워드_포함_모든_문서를_후보로_가져온다
│   └── topK가_컬렉션_크기보다_크면_있는_문서_전부를_반환한다
└── [하이브리드 보완]
    └── Dense_threshold_미달_문서를_Sparse가_키워드로_보완한다
```

**실행**: `./gradlew test --tests "*.RagRetrievalServiceIntegrationTest"`

---

### #63 — RagRetrievalServiceTest.java (기존 파일에 추가)

**목적**: `mergeWithRRF()` 로직의 k값 민감도를 코드 레벨에서 검증. package-private으로 가시성 임시 변경 후 테스트.

기존 `RagRetrievalServiceTest`에 `@Nested` 블록 추가:

```
RrfKValueTest  (Nested — 기존 파일 내)
├── k값에_무관하게_양쪽_등장_문서가_항상_단독_등장_문서보다_높은_점수를_받는다
├── k_10_30_60_100에서_Top4_구성이_동일하다  (안정성 확인)
└── Dense_1위_Sparse_1위가_서로_다를때_RRF_1위는_양쪽_등장_문서다
```

**실행**: `./gradlew test --tests "*.RagRetrievalServiceTest"`

---

## 수동 실험 (#57, #59, #60, #62)

### #57 — NotePreprocessor 임베딩 품질 비교

**준비**: 코드 블록 포함 노트 10개, ChromaDB 테스트 컬렉션 2개

| 단계 | 작업 |
|------|------|
| 1 | `NoteEmbeddingService`에서 `NotePreprocessor` 호출 임시 제거 → 10개 노트 임베딩 (`test-without-preprocessor`) |
| 2 | `NotePreprocessor` 활성화 → 동일 노트 재임베딩 (`test-with-preprocessor`) |
| 3 | 정답 쿼리 10개로 각 컬렉션 검색, distance 기록 |
| 4 | Recall@4, 평균 distance 비교 |

**통과 기준**: 코드 블록 포함 노트에서 전처리 적용 조건의 Recall@4가 0.1(10%p) 이상 높으면 효과 확인

---

### #59 — isTopicRelevant 2차 필터 노이즈 감소 측정

**준비**: `RagRetrievalService.retrieve()`에 임시 로그 추가

```java
log.info("[RAG 필터] 1차 통과: {}건, 2차 통과: {}건, 제거율: {:.1f}%",
    candidates.size(), afterSecondFilter,
    (candidates.size() - afterSecondFilter) * 100.0 / candidates.size());
```

| 단계 | 작업 |
|------|------|
| 1 | 노트 20개 이상 임베딩, 대화 시작 API 10회 이상 호출 |
| 2 | 서버 로그에서 `[RAG 필터]` 라인 수집 |
| 3 | 평균 제거율 계산, 제거된 문서 샘플 10건 수동 확인 |

**판단 기준**

| 2차 필터 제거율 | 해석 | 조치 |
|--------------|------|------|
| < 5% | 필터 거의 동작 안 함 | 임계값 재검토 또는 필터 제거 검토 |
| 5 ~ 30% | 적절히 동작 | 현행 유지 |
| > 30% | 과도한 문서 제거 | 임계값(0.2) 하향 검토 |

---

### #60 — similarity-threshold 최적값 검증

**준비**: 다양한 주제 노트 20개 이상, ground truth(정답 연관 노트 목록) 수동 작성

```yaml
# application.yml 변경 후 각 조건 실험
rag:
  similarity-threshold: 0.60  # 실험 1
  # similarity-threshold: 0.70  # 실험 2
  # similarity-threshold: 0.75  # 현재 기준선
  # similarity-threshold: 0.80  # 실험 3
  # similarity-threshold: 0.85  # 실험 4
```

각 임계값에서 Precision@4 / Recall@4 측정 후 F1-score 비교.

**채택 기준**: 현재 0.75보다 F1이 0.05 이상 높은 값이 있으면 변경 권장

---

### #62 — Dense-only vs 하이브리드 Recall@4 비교

**준비**: 카테고리별 노트 세트
- **카테고리 A** (코드 식별자 핵심): `HashMap`, `factorial`, `BinarySearch` 포함 노트 10개
- **카테고리 B** (짧은 한국어 CS 용어 핵심): "큐", "스택", "힙", "트리" 포함 노트 10개

| 단계 | 작업 |
|------|------|
| 1 | `bm25-max-results: 0` 으로 설정 (Sparse 비활성화) → Dense-only 조건 실행 |
| 2 | 카테고리별 쿼리 10개씩 실행, Recall@4 기록 |
| 3 | 하이브리드 활성화 후 동일 쿼리 재실행, Recall@4 기록 |
| 4 | 두 조건 비교 |

**채택 기준**: 카테고리 A 또는 B에서 하이브리드 Recall@4 차이 ≥ 0.1(10%p) → 도입 효과 확인

---

## 실행 순서 권장

```
1단계 (빠른 자동화 먼저)
  #58 → NotePreprocessorTest + NoteMetadataExtractorTest  (Mock 불필요, 즉시 실행)
  #63 → RagRetrievalServiceTest RRF 케이스 추가           (기존 파일 확장)

2단계 (ChromaDB 연동)
  #61 → RagRetrievalServiceIntegrationTest               (Testcontainers 셋업 필요)

3단계 (수동 실험 — ChromaDB에 실 데이터 필요)
  #59 → 2차 필터 로그 관찰                               (로그 추가만으로 바로 시작 가능)
  #60 → threshold 튜닝                                   (노트 20개 + ground truth 필요)
  #62 → Dense vs 하이브리드 비교                         (카테고리별 노트 세트 필요)
  #57 → NotePreprocessor 품질 비교                       (#62 데이터 재활용 가능)
```
