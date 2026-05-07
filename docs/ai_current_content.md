# AI 튜터 현재 동작 흐름 및 설계 결정

## 전체 데이터 흐름

```
sendMessage 요청
    │
    ▼
AiTutorService.respond()
    │
    ├── RagRetrievalService.retrieve(noteContent, userId)
    │       │
    │       ▼
    │   ChromaDB 유사도 검색 (user_id 필터)
    │       │
    │       ├── 결과 있음 → 1차: similarity threshold (0.75)
    │       │              2차: 키워드 오버랩 필터 (isTopicRelevant)
    │       │              → {rag_context} 주입
    │       │
    │       └── 결과 없음 → "(No relevant past notes found.)" 주입
    │
    ├── FocusEventFormatter.format(sessionId)
    │       └── 최근 15분 FOCUS_EVENT 포맷 → {focus_events} 주입
    │
    ▼
System Prompt 변수 치환
    {current_note} + {rag_context} + {focus_events} + {conversation_type} + {question_count}
    │
    ▼
OpenAI GPT-4o 호출 → TutorResponse 반환
```

---

## 노트 임베딩 흐름

```
노트 A 저장 → NoteEmbeddingService.embed(A) → ChromaDB "note-1" 저장
노트 B 저장 → NoteEmbeddingService.embed(B) → ChromaDB "note-2" 저장
노트 C 저장 → NoteEmbeddingService.embed(C) → ChromaDB "note-3" 저장
```

- ChromaDB에는 해당 유저가 저장한 **모든 노트**가 각각 독립 Document로 쌓임
- RAG 검색 시 유저의 모든 노트를 대상으로 유사도 검색

> **현재 상태:** `NoteEmbeddingService.embed()`가 어디서도 호출되지 않아 ChromaDB가 비어 있음.
> AI 응답은 RAG 없이 현재 노트 내용만으로 동작 중. → [note-feature-todo.md](note-feature-todo.md) 참고

---

## RAG 설계 결정 사항

### 1. 질문 메시지는 임베딩하지 않는다

`AiMessage` (사용자/AI 대화 메시지)는 ChromaDB에 임베딩하지 않는다.

**이유:**
- 같은 대화 내 히스토리는 `AiTutorService`에서 DB 직접 조회 후 LLM에 주입하고 있음
- RAG의 목적은 **과거 노트에서 관련 학습 내용 검색**이며, 질문 문장은 이 목적에 맞지 않음
- 임베딩 시 "왜 재귀가 헷갈리죠?" 같은 질문 문장이 노트 검색 결과에 섞여 RAG 노이즈 발생

**추후 확장 고려:** 세션 간 학습 패턴 추적이 필요해지는 시점에, 메시지 단위 임베딩이 아니라 **세션 요약본 생성 후 임베딩**하는 방식 권장.

---

### 2. 청킹(Chunking)을 적용하지 않는다

노트를 분할하지 않고 전체를 하나의 Document로 임베딩한다.

**이유:** RAG 쿼리가 **노트 전체 내용**이기 때문이다.

```java
// AiTutorService에서
ragRetrievalService.retrieve(note.getContent(), userId)
//                   ↑ 현재 노트 전체를 쿼리로 사용
```

쿼리가 단어/문장이 아닌 노트 전체 → **노트 ↔ 노트 간 토픽 유사도** 비교.
청킹은 짧은 쿼리로 긴 문서에서 관련 단락을 찾을 때 유효한 기법이라 구조가 맞지 않음.

| 상황 | 지금 (전체 문서) | 청킹 시 |
|------|----------------|---------|
| "재귀 함수 노트" 쿼리 | 비슷한 토픽의 과거 노트 전체 반환 | "base case" 단락 등 파편화된 조각 반환 |
| AI 튜터 활용 | 과거 학습 맥락을 풍부하게 이해 | 문맥 없는 단편만 받아서 노이즈 증가 |

**긴 노트 판단 기준:**

| 노트 길이 | 권장 방식 |
|-----------|----------|
| ~500 토큰 이하 | 전체 임베딩 (현재 방식) |
| 500~1500 토큰 | 전체 임베딩 + 주입 시 길이 제한 |
| 1500 토큰 초과 | 마크다운 헤더 기준 섹션 청킹 고려 |

청킹 도입 시 **Parent Document Retrieval 패턴** 필요 (청크로 검색 → 부모 노트 전체 반환).

---

### 3. 임베딩 범위는 노트 단위 (독립 저장)

- 노트 저장 시 해당 노트만 임베딩
- ChromaDB에는 유저의 모든 노트가 독립 Document로 누적
- RAG 검색 시 `user_id` 필터로 해당 유저 노트만 대상으로 검색 → 유저 간 데이터 격리 보장

---

## 관련 파일

| 역할 | 파일 경로 |
|------|-----------|
| AI 튜터 오케스트레이션 | `global/ai/tutor/AiTutorService.java` |
| 임베딩 저장/삭제 | `global/ai/rag/NoteEmbeddingService.java` |
| RAG 검색 | `global/ai/rag/RagRetrievalService.java` |
| 검색 결과 포맷 | `global/ai/rag/RagContext.java` |
| 집중 이벤트 포맷 | `global/ai/rag/FocusEventFormatter.java` |
| 시스템 프롬프트 템플릿 | `resources/prompts/tutor-system.st` |
