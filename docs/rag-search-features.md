# RAG 검색 기능 명세

> **대상**: `global/ai/rag/` 패키지  
> **관련 브랜치**: `develop` (기본 RAG), `feat/hybrid-search-bm25` (하이브리드 검색 개선)

---

## 목차

1. [검색 기능 도입 배경](#1-검색-기능-도입-배경)
2. [전체 검색 파이프라인 구조](#2-전체-검색-파이프라인-구조)
3. [각 기능의 역할](#3-각-기능의-역할)
4. [의도대로 작동하는지 확인한 방법과 결과](#4-의도대로-작동하는지-확인한-방법과-결과)
5. [향후 테스트 계획](#5-향후-테스트-계획)
6. [설정값 요약](#6-설정값-요약)

---

## 1. 검색 기능 도입 배경

### 1.1 기본 RAG 파이프라인 (4개 기능)

Critical-Flow는 학습자가 노트를 작성하면 AI 튜터가 소크라테스식 질문을 생성하는 시스템이다.  
AI가 단순히 현재 노트에 대한 질문만 하는 것이 아니라, **과거 학습 내용과 연결된 질문**(TYPE_C: Spaced Recall)을 하려면 다음 조건이 필요하다.

| 필요 조건 | 구현 방법 |
|---------|---------|
| 과거 노트를 의미론적으로 검색 | `NoteEmbeddingService` + `RagRetrievalService` (벡터 유사도 검색) |
| 코드가 섞인 노트도 정확하게 임베딩 | `NotePreprocessor` (코드 블록 전처리) |
| 검색 결과를 System Prompt에 주입 | `RagContext` (직렬화 포맷) |
| 학습자 집중 상태를 AI에게 전달 | `FocusEventFormatter` (IoT 이벤트 조회) |

단순 키워드 검색 대신 벡터 유사도 검색을 선택한 이유는, 학습 노트에서 동일한 개념이 다양한 표현으로 기록되기 때문이다.  
예: "재귀" ↔ "recursive call" ↔ "스스로를 호출하는 함수" → 키워드 검색은 이를 연결하지 못하지만, 의미 벡터 검색은 연결 가능하다.

### 1.2 하이브리드 검색 추가 (`feat/hybrid-search-bm25`)

기본 RAG의 Dense-only 검색은 아래 케이스에서 검색 누락이 발생했다.

| 취약 케이스 | 원인 | 예시 |
|-----------|------|------|
| 정확한 함수명·클래스명 | 고유 식별자(`HashMap`, `factorial`)는 임베딩 공간에서 일반 단어와 의미 거리 구분이 어렵다 | "HashMap 사용법" 노트가 "자료구조 요약" 노트보다 유사도가 낮게 측정돼 누락 |
| 짧은 한국어 CS 용어 | "큐", "스택" 같은 1자 단어는 임베딩 벡터에서 의미 구분이 뭉개진다 | "큐를 공부했다" 노트가 threshold 0.75를 넘지 못해 누락 |

**해결 방향**: Sparse(키워드 기반) 검색을 병렬로 실행해 Dense가 놓친 문서를 보완하고, RRF(Reciprocal Rank Fusion)로 두 결과를 병합한다.  
양쪽 검색에서 모두 높은 순위를 받은 문서가 자동으로 상위로 올라오는 구조다.

---

## 2. 전체 검색 파이프라인 구조

### 2.1 임베딩 파이프라인 — 노트 저장 시

```
[학습자] 노트 생성/수정
    ↓
NoteService.saveNote() or updateNote()
    ↓
NoteEmbeddingService.embed(StudyNote note)
    ├─ NotePreprocessor.preprocessForEmbedding()
    │      코드 블록 → 식별자 텍스트 변환
    │      예: ```python\ndef fib(n):``` → [python: fib]
    │
    ├─ NoteMetadataExtractor.extractLanguages()   → ["python", "java"]
    ├─ NoteMetadataExtractor.extractHeaders()     → ["개요", "구현", "정리"]
    │
    └─ VectorStore.add(Document)
           id:       "note-{noteId}"
           content:  전처리된 노트 내용
           metadata: note_id, session_id, user_id, title,
                     created_at, languages, headers
               ↓
           ChromaDB에 벡터 저장

[학습자] 노트 삭제
    ↓
NoteEmbeddingService.delete(noteId)
    ↓
VectorStore.delete("note-{noteId}")
    ↓
ChromaDB에서 벡터 제거
```

### 2.2 검색 파이프라인 — 대화 시작 / 메시지 응답 시

**기본 RAG (Dense-only, `develop` 기준)**

```
AiTutorService.generateFirstQuestion() or respond()
    ↓
RagRetrievalService.retrieve(queryText, userId, excludeNoteId)
    │
    ├─ VectorStore.similaritySearch()
    │       filterExpression: user_id == userId && note_id != excludeNoteId
    │       similarityThreshold: 0.75
    │       topK: maxResults + 2 (= 6)
    │
    ├─ [1차 필터] similarity ≥ 0.75 — ChromaDB 수준 유사도 필터
    │
    ├─ [2차 필터] isTopicRelevant() — 키워드 오버랩 ≥ 20%
    │
    └─ limit(4) → RagContext 반환
```

**하이브리드 검색 (`feat/hybrid-search-bm25`)**

```
RagRetrievalService.retrieve(queryText, userId, excludeNoteId)
    │
    ├─ denseSearch()
    │       VectorStore.similaritySearch(threshold=0.75, topK=6)
    │       → Dense 결과 리스트
    │
    ├─ sparseSearch()
    │       extractKeyTerms(queryText) → 의미 있는 키워드 목록
    │       VectorStore.similaritySearch(threshold=0.0, topK=10)  ← 넓게 후보 수집
    │       keywords.anyMatch(content::contains)                   ← 키워드 포함 여부 필터
    │       → Sparse 결과 리스트
    │
    ├─ mergeWithRRF(dense, sparse, limit=4)
    │       각 문서의 점수 = Σ 1/(k + rank),  k=60
    │       양쪽에 등장한 문서 → 점수 합산 → 상위 정렬
    │       → 병합된 상위 4개 문서
    │
    ├─ [2차 필터] isTopicRelevant() — 키워드 오버랩 ≥ 20%
    │
    └─ RagContext 반환
```

### 2.3 AI 응답 생성 흐름 (RAG 결과 활용)

```
RagContext.format()
    → {rag_context} 변수 문자열 생성

FocusEventFormatter.format(sessionId)
    → FocusEventRepository.findBySessionIdAndDetectedAtAfterOrderByDetectedAtAsc()
    → {focus_events} 변수 문자열 생성

QuestionTypeRouter.route(note, ragContext)
    → 질문 타입 결정 (TYPE_A ~ TYPE_F)

ChatClient.prompt()
    → System Prompt에 {current_note}, {rag_context}, {focus_events},
                      {selected_question_type} 주입
    → GPT-4o 응답 생성
```

---

## 3. 각 기능의 역할

### 3.1 NoteEmbeddingService — 벡터 임베딩 저장/삭제

**파일**: `global/ai/rag/NoteEmbeddingService.java`

노트가 생성·수정·삭제될 때 ChromaDB 벡터 스토어와의 동기화를 담당한다.  
직접 임베딩하기 전에 `NotePreprocessor`와 `NoteMetadataExtractor`를 호출해 저장 품질을 높인다.

| 메서드 | 호출 시점 | 동작 |
|--------|---------|------|
| `embed(StudyNote note)` | 노트 생성/수정 후 | 기존 벡터 삭제 → 전처리 → 메타데이터 추출 → ChromaDB 저장 |
| `delete(Long noteId)` | 노트 삭제 후 | ChromaDB에서 해당 벡터 제거 |

`embed()` 실행 순서:

```
1. vectorStore.delete("note-{noteId}")         ← 수정 시 구버전 벡터 먼저 제거
2. NotePreprocessor.preprocessForEmbedding()  ← 코드 블록 전처리
3. NoteMetadataExtractor.extractLanguages()   ← 사용 언어 목록 추출
4. NoteMetadataExtractor.extractHeaders()     ← 마크다운 헤더 추출
5. Document 생성 (id, content, metadata)
6. vectorStore.add()                          ← ChromaDB 저장
```

저장되는 메타데이터:

| 키 | 값 예시 | 용도 |
|----|--------|------|
| `note_id` | `"42"` | 자기 참조 방지 필터 (`excludeNoteId`) |
| `user_id` | `"7"` | 사용자 격리 필터 |
| `session_id` | `"15"` | RagContext 포맷 출력 |
| `title` | `"HashMap 정리"` | RagContext 포맷 출력 |
| `created_at` | `"2026-05-07T..."` | 향후 시간 기반 필터용 |
| `languages` | `"python,java"` | 향후 언어 기반 필터용 |
| `headers` | `"개요,구현,정리"` | 향후 헤더 기반 필터용 |

---

#### 3.1.1 NotePreprocessor — 코드 블록 전처리

**파일**: `global/ai/rag/NotePreprocessor.java`

마크다운 노트의 코드 블록을 의미 있는 식별자 텍스트로 변환한다.

**도입 이유**: 코드 블록을 원문 그대로 임베딩하면 `{`, `;`, `}`, `()` 같은 구문 기호가 벡터 공간을 차지해 실제 의미 정보가 희석된다. 한국어 설명과 코드가 혼합된 노트에서 특히 문제가 된다.

**변환 예시**:

```
[입력]
재귀 함수란 자기 자신을 호출하는 함수다.

```python
def fibonacci(n):
    if n <= 1:
        return n
    return fibonacci(n-1) + fibonacci(n-2)
```

[출력 (임베딩 대상)]
재귀 함수란 자기 자신을 호출하는 함수다.
[python: fibonacci return]
```

변환 규칙:
- 코드 블록 전체를 `[언어: 식별자1 식별자2 ...]` 형태로 압축
- 식별자 추출 기준: 영문/한글 3자 초과 토큰, 중복 제거
- 언어 표시가 없으면 `[code: ...]` 사용

---

#### 3.1.2 NoteMetadataExtractor — 메타데이터 추출

**파일**: `global/ai/rag/NoteMetadataExtractor.java`

노트에서 프로그래밍 언어 목록과 마크다운 헤더를 추출해 메타데이터로 저장한다.

| 메서드 | 추출 방법 | 결과 예시 |
|--------|---------|---------|
| `extractLanguages(markdown)` | 코드 블록 ` ```언어명 ` 패턴 정규식 파싱 | `["python", "java"]` |
| `extractHeaders(markdown)` | `#`으로 시작하는 줄 파싱 | `["개요", "핵심 개념", "예제"]` |

특이 케이스: 언어 미지정 코드 블록(` ``` `)만 존재하면 `["unknown"]` 반환.

**현재 용도**: `languages`, `headers` 메타데이터는 현재 ChromaDB 검색 필터에 사용하지 않는다.  
향후 "동일한 언어를 사용한 노트만 검색" 또는 "특정 헤더를 포함한 노트만 검색" 기능 확장을 위한 사전 준비다.

---

### 3.2 RagRetrievalService — 유사도 기반 검색

**파일**: `global/ai/rag/RagRetrievalService.java`

현재 노트 내용을 쿼리로 사용해 ChromaDB에서 관련 과거 노트를 검색한다.  
`develop` 기준과 `feat/hybrid-search-bm25` 기준에 따라 검색 방식이 다르다.

---

#### 3.2.1 기존 방식: Dense-only 검색 (`develop` 기준)

```
public RagContext retrieve(String queryText, Long userId, Long excludeNoteId)
    │
    ├─ [메타데이터 필터]
    │       "user_id == '{userId}' && note_id != '{excludeNoteId}'"
    │       - 다른 사용자 노트 완전 차단 (사용자 데이터 격리)
    │       - 현재 노트 자신 제외 (자기 자신은 항상 similarity 1.0 → 무의미한 매칭 방지)
    │
    ├─ [1차 필터] VectorStore.similaritySearch(threshold=0.75)
    │       - ChromaDB 벡터 공간에서 코사인 유사도 계산
    │       - 유사도 0.75 미만 문서는 ChromaDB 단계에서 제거
    │       - topK=6 (병합 여유분 확보)
    │
    ├─ [2차 필터] isTopicRelevant(doc, queryText)
    │       - 쿼리의 의미 있는 키워드(한국어 1자↑, 영어 3자↑) 추출
    │       - 해당 키워드 중 20% 이상이 문서 내용에 포함되어야 통과
    │       - 목적: threshold를 간신히 넘긴 토픽 불일치 문서 제거
    │         (예: "Java GC" 노트 ↔ "Python 가비지 컬렉션" 노트처럼
    │              벡터 유사도는 높지만 실제 주제가 다른 경우)
    │
    └─ limit(4) → RagContext 반환
```

---

#### 3.2.2 개선 방식: 하이브리드 검색 (`feat/hybrid-search-bm25`)

Dense-only의 검색 누락 문제를 해결하기 위해 Sparse 검색을 추가하고 RRF로 병합한다.

**Dense 검색 — `denseSearch()`**

기존 방식과 동일하다. 의미론적 벡터 유사도 기반이며 threshold 0.75 이상만 통과한다.

```
VectorStore.similaritySearch(threshold=0.75, topK=6)
filterExpression: user_id == userId && note_id != excludeNoteId
```

**Sparse 검색 — `sparseSearch()` + `extractKeyTerms()`**

Dense가 놓친 "정확한 단어 일치" 기반 문서를 찾는다.

```
extractKeyTerms(queryText)
    한국어 포함 시: 1자 이상 토큰 추출
    영어만 있을 때: 4자 이상 토큰 추출
    → 중복 제거, 소문자 변환
    예: "HashMap 자료구조 get" → ["hashmap", "자료구조", "get"] (영어는 4자 이상)
        → 실제 필터: ["hashmap", "자료구조"]

VectorStore.similaritySearch(threshold=0.0, topK=10)
    threshold=0.0 이유: 의미 유사도와 무관하게 넓은 후보를 수집하기 위함
    threshold를 높이면 Dense와 같은 문서만 나와 Sparse가 보완 역할을 못 함

candidates.filter(keywords.anyMatch(content::contains))
    키워드 중 하나라도 포함된 문서만 통과
```

**버그 수정 이력**: `extractKeyTerms` 초기 구현에서 한국어 최소 길이를 `2`로 설정해 "큐", "스택" 같은 1자 CS 용어가 키워드에서 제외됐다 → `1`로 수정 (commit `4ff50af`).

**RRF 병합 — `mergeWithRRF()`**

Dense 결과와 Sparse 결과를 순위 기반으로 합산한다.

```
RRF 점수 공식: score(문서) = Σ ( 1 / (k + rank) ),  k = 60

예시:
  문서 A — Dense 1위, Sparse 1위 → 1/61 + 1/61 = 0.0328  ← 최상위
  문서 B — Dense 2위, Sparse 없음 → 1/62 = 0.0161
  문서 C — Dense 없음, Sparse 2위 → 1/62 = 0.0161
  
→ A > B = C 순서로 정렬
```

- `k = 60`: RRF 논문 기본값. 순위 1위와 2위의 점수 차이를 완화해 순위 역전을 방지하는 완충값이다.
- Dense와 Sparse 양쪽에 등장한 문서가 두 점수를 합산받아 자연스럽게 상위에 오른다.
- 어느 한쪽에만 등장해도 결과에 포함되므로 Dense-only와 Sparse-only 문서 모두 커버한다.

**2차 필터 — `isTopicRelevant()` (기존 유지)**

RRF 병합 이후에도 키워드 오버랩 20% 필터를 그대로 적용한다.  
Sparse 검색으로 후보가 더 넓어졌기 때문에, 최종 단계에서 노이즈를 한 번 더 걸러내는 역할이 더 중요해진다.

---

### 3.3 RagContext — 검색 결과 직렬화

**파일**: `global/ai/rag/RagContext.java`

`RagRetrievalService.retrieve()`의 반환 타입. 검색된 청크 목록을 들고 있으며, `format()`을 호출하면 System Prompt의 `{rag_context}` 변수에 주입할 문자열을 생성한다.

**`format()` 출력 예시 (검색 결과 있을 때)**:

```
[참고용 과거 노트 — 현재 노트 학습 보조 목적으로만 활용]

--- [Note: "HashMap 정리" | session_id: 42 | similarity: 0.83] ---
HashMap은 키-값 쌍을 저장하는 자료구조다. put(), get(), remove() 메서드를 제공한다.

--- [Note: "재귀 알고리즘" | session_id: 38 | similarity: 0.79] ---
재귀 함수는 자기 자신을 호출하는 함수다. Base case를 반드시 정의해야 무한 루프를 방지할 수 있다.
```

**`format()` 출력 (검색 결과 없을 때)**:

```
(No relevant past notes found.)
```

`isEmpty()` 메서드로 결과 유무를 확인할 수 있으며, `QuestionTypeRouter`에서 TYPE_C 선택 가능 여부를 판단할 때 사용한다.

**레이블 텍스트를 명시한 이유**: 초기 구현에서 레이블 없이 청크 내용만 주입했을 때, LLM이 과거 노트를 "현재 노트의 일부"로 오해해 해당 내용을 기반으로 직접 답을 제공하는 현상이 발생했다.  
`[참고용 과거 노트 — 현재 노트 학습 보조 목적으로만 활용]` 레이블이 과거 노트의 역할을 LLM에게 명시적으로 알려, LAW 4(RAG 노이즈 거부)와 함께 동작한다.

---

### 3.4 FocusEventFormatter — 집중 이탈 이벤트 조회 및 포맷

**파일**: `global/ai/rag/FocusEventFormatter.java`

IoT 디바이스(카메라 기반 집중도 추적)가 기록한 집중 이탈 이벤트를 DB에서 조회하고, System Prompt의 `{focus_events}` 변수에 주입할 텍스트로 포맷팅한다.

**조회 조건**: 현재 세션(`sessionId`)의 최근 N분(`focus-lookback-minutes`, 기본값 15분) 이내 이벤트

**출력 예시 (이벤트 있을 때)**:

```
[GAZE_OUT | 8s | alerted=false]
[DROWSY | 23s | alerted=true]
[ABSENT | 45s | alerted=true]
```

**출력 (이벤트 없을 때)**:

```
(No focus events in the last 15 minutes.)
```

**도입 이유**: 학습자가 집중을 잃은 시점에 어떤 개념을 보고 있었는지를 AI가 알면, 해당 개념을 재확인하는 질문을 생성할 수 있다. 단, System Prompt LAW 5(집중도 이벤트 절제된 활용)에 따라 AI는 이 데이터를 매 응답마다 활용하지 않고, 유의미한 패턴(긴 이탈, 반복 이탈 등)이 있을 때만 반영한다.

이벤트 타입:

| 타입 | 의미 |
|------|------|
| `GAZE_OUT` | 시선이 화면 밖으로 이탈 |
| `DROWSY` | 졸음 감지 |
| `ABSENT` | 자리 이탈 |

---
