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

> 각 이슈별 상세 테스트 결과는 아래 개별 파일을 참고한다.
> - [#57 — NotePreprocessor 임베딩 품질 비교](../테스트보고서/57_NotePreprocessor_임베딩_품질_비교.md)
> - [#58 — NotePreprocessor / NoteMetadataExtractor 단위 테스트](../테스트보고서/58_NotePreprocessor_단위_테스트.md)
> - [#59 — isTopicRelevant 2차 필터 노이즈 감소 효과](../테스트보고서/59_2차_필터_노이즈_감소_측정.md)
> - [#60 — similarity-threshold 최적값 검증](../테스트보고서/60_similarity_threshold_최적값_검증.md)
> - [#61 — RagRetrievalService ChromaDB 통합 테스트](../테스트보고서/61_RagRetrievalService_ChromaDB_통합_테스트.md)
> - [#62 — Dense-only vs 하이브리드 Recall@4 비교](../테스트보고서/62_Dense_vs_하이브리드_Recall_비교.md)
> - [#63 — RRF k=60 최적값 검증](../테스트보고서/63_RRF_k60_최적값_검증.md)

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
  #60 → 60_similarity_threshold_최적값_검증.md 참고
  #62 → 62_Dense_vs_하이브리드_Recall_비교.md 참고
```

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

## 잔여 과제

| 항목 | 내용 |
|------|------|
| RagRetrievalServiceTest 임계값 수정 | `setUp()`의 `similarityThreshold=0.75` → `0.55`, 스텁 조건 `>= 0.75` → `>= 0.55` |
| `[#62]` 측정 로그 제거 | `RagRetrievalService.java` Dense/Sparse 건수 로그, 최종 Top-4 로그 |
| noteId=10(힙) 2차 필터 과도 제거 | `isTopicRelevant()` 전문 용어 노트 대응 방안 검토 |
