# #57 — NotePreprocessor 임베딩 품질 비교 측정 결과

## 수동 측정 조건

| 항목 | 값 |
|------|-----|
| 측정 일자 | 2026-05-11 |
| 임베딩 모델 | `text-embedding-3-small` (OpenAI) |
| 벡터 DB | ChromaDB 1.0.0 (Testcontainers) |
| 테스트 노트 수 | 10개 (모두 코드 블록 포함) |
| 쿼리 수 | 10개 (노트별 1:1 정답 쿼리) |
| 평가 지표 | Recall@4 (topK=4, similarityThreshold=0.0) |
| 통과 기준 | 전처리 적용 Recall@4 − 미적용 Recall@4 ≥ 10%p |

**테스트 노트 목록 (코드 블록 포함 CS 주제)**

| ID | 제목 | 정답 쿼리 |
|----|------|----------|
| qnote-1 | HashMap 자료구조 | HashMap put get containsKey 사용법 |
| qnote-2 | 재귀 함수 팩토리얼 | 재귀 호출 팩토리얼 기저 조건 구현 |
| qnote-3 | 이진 탐색 알고리즘 | 정렬된 배열 이진 탐색 left right mid 구현 |
| qnote-4 | SQL JOIN 쿼리 | SQL INNER JOIN LEFT JOIN 테이블 조인 쿼리 |
| qnote-5 | 버블 정렬 알고리즘 | 버블 정렬 swap 인접 비교 알고리즘 구현 |
| qnote-6 | 스프링 의존성 주입 | 스프링 Autowired 빈 의존성 주입 Service Repository |
| qnote-7 | 동적 프로그래밍 메모이제이션 | 메모이제이션 dp 피보나치 동적 프로그래밍 최적화 |
| qnote-8 | BFS 그래프 탐색 | BFS 너비 우선 탐색 큐 Queue LinkedList 구현 |
| qnote-9 | 스택 자료구조 | 스택 LIFO push pop peek 괄호 매칭 구현 |
| qnote-10 | 자바 예외 처리 | 자바 예외 처리 try catch finally IOException RuntimeException |

---

## 1차 측정 결과 (NotePreprocessor 적용 — 수정 전 상태)

`NoteEmbeddingService`가 `notePreprocessor.preprocessForEmbedding()`을 호출하는 현재 코드 상태.  
코드 블록이 `[java: HashMap put get ...]` 형태 식별자 목록으로 변환된 뒤 임베딩됨.

| 쿼리 노트 | 결과 | top4 |
|-----------|------|------|
| qnote-1 (HashMap) | **miss** | qnote-7, qnote-4, qnote-10, qnote-2 |
| qnote-2 (재귀) | **miss** | qnote-7, qnote-10, qnote-4, qnote-5 |
| qnote-3 (이진탐색) | **miss** | qnote-4, qnote-7, qnote-9, qnote-6 |
| qnote-4 (SQL) | HIT | qnote-4, qnote-7, qnote-9, qnote-2 |
| qnote-5 (버블정렬) | **miss** | qnote-7, qnote-9, qnote-4, qnote-10 |
| qnote-6 (스프링 DI) | **miss** | qnote-10, qnote-4, qnote-7, qnote-3 |
| qnote-7 (DP) | HIT | qnote-7, qnote-9, qnote-10, qnote-4 |
| qnote-8 (BFS) | **miss** | qnote-9, qnote-7, qnote-10, qnote-2 |
| qnote-9 (스택) | HIT | qnote-9, qnote-7, qnote-4, qnote-6 |
| qnote-10 (예외처리) | HIT | qnote-10, qnote-9, qnote-7, qnote-8 |

**Recall@4: 40% (4/10)**

---

## 문제 원인 분석

### 1. qnote-7이 '허브'가 되는 오염 현상

전처리 후 qnote-7(DP/메모이제이션)의 임베딩 대상 텍스트는 다음과 같다:

```
[java: HashMap memo containsKey fibonacci climbStairs result DynamicProgramming]
```

이 식별자들은 여러 주제와 교차한다.

| 식별자 | 원래 주제 |
|--------|----------|
| `HashMap`, `containsKey` | qnote-1 (HashMap) |
| `fibonacci` | qnote-2 (재귀) |
| `result`, `climbStairs` | 여러 노트 |

결과적으로 qnote-7의 임베딩 벡터가 10개 쿼리 중 6개의 top4에 잘못 등장한다.

### 2. 전처리가 모델의 강점을 역이용

`text-embedding-3-small`은 코드와 자연어가 혼합된 문서를 원래부터 잘 처리하도록 훈련되어 있다. 코드 블록을 식별자 목록으로 압축하면:

- 코드 내 문맥 관계(함수 시그니처, 자료 흐름 등)가 소실된다.
- 공통 변수명(`result`, `temp`, `arr`, `n`, `i`)이 여러 노트 간 거짓 유사도를 유발한다.
- 한국어 설명과 식별자 목록만 남아 문서 표현이 오히려 빈약해진다.

### 3. 원래 가설의 오류

> "코드 블록이 한국어 설명과 섞이면 벡터가 희석된다"

이 가설은 Word2Vec 시대의 단순 평균 임베딩에서는 유효하지만, `text-embedding-3-small`처럼 문맥 전체를 보는 모델에서는 코드 원문 유지가 오히려 더 정확한 표현을 만든다.

---

## 수정 내용

`NoteEmbeddingService.embed()`에서 `notePreprocessor` 호출을 제거하고 원본 마크다운을 그대로 임베딩한다.

```java
// 수정 전
String processedContent = notePreprocessor.preprocessForEmbedding(note.getContent());

// 수정 후
String processedContent = note.getContent();
```

---

## 2차 측정 결과 (NotePreprocessor 미적용 — 수정 후 상태)

동일 테스트 클래스의 Phase 1 결과 (원본 마크다운 임베딩).

| 쿼리 노트 | 결과 | top4 |
|-----------|------|------|
| qnote-1 (HashMap) | HIT | qnote-1, qnote-3, qnote-6, qnote-8 |
| qnote-2 (재귀) | HIT | qnote-2, qnote-7, qnote-6, qnote-10 |
| qnote-3 (이진탐색) | HIT | qnote-3, qnote-5, qnote-8, qnote-4 |
| qnote-4 (SQL) | HIT | qnote-4, qnote-3, qnote-8, qnote-9 |
| qnote-5 (버블정렬) | HIT | qnote-5, qnote-3, qnote-7, qnote-9 |
| qnote-6 (스프링 DI) | HIT | qnote-6, qnote-10, qnote-4, qnote-1 |
| qnote-7 (DP) | HIT | qnote-7, qnote-2, qnote-5, qnote-3 |
| qnote-8 (BFS) | HIT | qnote-8, qnote-3, qnote-9, qnote-5 |
| qnote-9 (스택) | HIT | qnote-9, qnote-7, qnote-3, qnote-8 |
| qnote-10 (예외처리) | HIT | qnote-10, qnote-9, qnote-6, qnote-7 |

**Recall@4: 100% (10/10)**

---

## 해석

| 지표 | 전처리 적용 (수정 전) | 전처리 미적용 (수정 후) |
|------|----------------------|------------------------|
| Recall@4 | 40% | **100%** |
| 개선폭 | — | **+60pp** |
| 통과 기준 (≥ 10pp) | FAIL | **PASS** |

- 전처리 적용 시 Recall@4가 40%로 저하되는 이유는 식별자 기반 압축이 `text-embedding-3-small`의 문맥 이해를 방해하기 때문이다.
- 전처리 미적용 시 10개 쿼리 전부 top4 안에 정답 노트가 포함된다. 현재 모델이 코드+한국어 혼합 문서를 충분히 처리할 수 있음을 보여준다.

---

## 결론

**`NotePreprocessor`는 `text-embedding-3-small` 환경에서 임베딩 품질을 저하시킨다. 비활성화가 필요하다.**

| 항목 | 결정 |
|------|------|
| NotePreprocessor 사용 여부 | **제거** |
| 근거 | 측정 기반 — Recall@4 −60pp 역효과 확인 |
| 영향 범위 | `NoteEmbeddingService.embed()` 한 줄 수정 |
| 기존 ChromaDB 데이터 | 재임베딩 필요 (전처리된 벡터가 이미 저장된 경우) |
