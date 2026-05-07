# 노트 기능 남은 작업

## 현재 상태 요약

> `NoteEmbeddingService.embed()`가 어디서도 호출되지 않음.  
> ChromaDB에 저장된 벡터가 없기 때문에, **현재 AI 응답은 RAG 없이 현재 노트 내용만으로 동작 중.**  
> RAG를 실제로 동작시키려면 노트를 ChromaDB에 임베딩하는 트리거가 필요하다.

---

## 작업 목록

### 1. NoteService 구현 (필수)

RAG 전체가 여기서 시작된다. 서비스 레이어 없이는 임베딩 트리거를 연결할 수 없음.

**구현할 메서드:**
- `saveNote(...)` → `noteRepository.save()` 후 `noteEmbeddingService.embed()` 호출
- `updateNote(...)` → `noteRepository.save()` 후 `noteEmbeddingService.embed()` 호출 (내부적으로 기존 벡터 삭제 후 재삽입 처리됨)
- `deleteNote(noteId)` → `noteRepository.delete()` 후 `noteEmbeddingService.delete(noteId)` 호출

**핵심 연결 포인트:**
```
noteRepository.save(note)
    ↓
noteEmbeddingService.embed(note)   ← 이 연결이 현재 없음
```

---

### 2. NoteController 구현

클라이언트가 노트를 저장할 수단이 없음.

**구현할 엔드포인트 (예시):**

| Method | URL | 설명 |
|--------|-----|------|
| `POST` | `/api/notes` | 노트 생성 |
| `PUT` | `/api/notes/{noteId}` | 노트 수정 |
| `DELETE` | `/api/notes/{noteId}` | 노트 삭제 |
| `GET` | `/api/notes?userId={userId}` | 유저 노트 목록 조회 |

---

### 3. StudyNoteRepository 쿼리 메서드 추가

현재 `JpaRepository` 기본 메서드만 있음. 목록 조회에 필요한 쿼리가 없음.

**추가 필요한 메서드:**
```java
List<StudyNote> findByUserIdOrderByCreatedAtDesc(Long userId);
List<StudyNote> findBySessionId(Long sessionId);
Optional<StudyNote> findByNoteIdAndUserId(Long noteId, Long userId);  // 권한 검증용
```

---

### 4. 삭제 시 임베딩 연동 확인

`NoteEmbeddingService.delete(noteId)`는 구현되어 있지만 호출되지 않음.  
노트 삭제 시 ChromaDB에 고아 벡터가 쌓이는 문제 발생.  
NoteService.deleteNote() 구현 시 반드시 포함.

---

### 5. RAG 컨텍스트 역할 명확화 — 시스템 프롬프트 보완

**원칙:**
- AI 튜터의 질문은 반드시 **현재 노트(`{current_note}`)** 를 기반으로 생성해야 한다.
- 과거 노트(`{rag_context}`)는 질문의 주제가 되어서는 안 되며, **연계 힌트나 맥락 보강**에만 사용한다.

**왜 필요한가:**

RAG가 정상 동작하면 과거 노트가 프롬프트에 함께 주입된다. 이 때 AI가 현재 노트보다 과거 노트 내용에 집중해 질문을 생성할 수 있다. 예를 들어 현재 "재귀 함수"를 공부 중인데, 과거 "스택" 노트가 높은 유사도로 검색되면 AI가 스택 관련 질문만 던지는 상황이 발생할 수 있다.

**튜터링 관점에서의 문제:**
- 학습자는 지금 재귀를 공부하고 있는데 스택 질문을 받으면 혼란스러움
- 현재 노트 내용을 제대로 이해했는지 확인하기 어려워짐
- 과거 내용이 주도권을 가져가면 소크라테스식 질문 흐름이 깨짐

**개선 방향 — `tutor-system.st` 프롬프트에 명시적 규칙 추가:**

```
- 질문은 반드시 {current_note} 내용에서 출발해야 한다.
- {rag_context}는 학습자가 관련 개념을 이전에 접했는지 확인하거나,
  연계 질문(TYPE C: Spaced Recall)을 보조할 때만 참고한다.
- {rag_context}를 단독 질문 소재로 사용하지 않는다.
```

### 6. `RagContext.format()` — 과거 노트 레이블 명시 (높음)

현재 `{rag_context}`로 주입되는 포맷에 용도 레이블이 없어, LLM이 과거 노트를 일반 컨텍스트와 동일하게 취급할 수 있다.

**수정 위치:** `global/ai/rag/RagContext.java` — `format()` 메서드

```java
// 변경 전
return chunks.stream()
        .map(c -> String.format(
                "--- [Note: \"%s\" | session_id: %s | similarity: %.2f] ---\n%s", ...))
        .collect(Collectors.joining("\n\n"));

// 변경 후
return "[참고용 과거 노트 — 현재 노트 학습 보조 목적으로만 활용]\n\n"
        + chunks.stream()
                .map(c -> String.format(
                        "--- [Note: \"%s\" | session_id: %s | similarity: %.2f] ---\n%s", ...))
                .collect(Collectors.joining("\n\n"));
```

프롬프트 규칙 단독으로 제한하는 것보다, `{rag_context}` 값 자체에 레이블을 달아두면 이중으로 강제된다.

---

### 7. `RagRetrievalService.retrieve()` — 현재 노트 자신 검색 결과 제외 (높음)

**왜 필요한가:**

NoteService 구현 후 노트 저장 시 `embed()`가 호출되면, 현재 노트 자신도 ChromaDB에 들어간다.  
이 상태에서 RAG 검색 시 현재 노트가 **similarity 1.0으로 1순위**로 잡혀 과거 노트 검색 결과를 밀어낸다.

**수정 위치 1:** `global/ai/rag/RagRetrievalService.java` — `retrieve()` 시그니처 및 필터 변경

```java
// 변경 전
public RagContext retrieve(String queryText, Long userId)

// 변경 후
public RagContext retrieve(String queryText, Long userId, Long excludeNoteId)
```

```java
// 변경 전
.filterExpression("user_id == '" + userId + "'")

// 변경 후
.filterExpression("user_id == '" + userId + "' && note_id != '" + excludeNoteId + "'")
```

**수정 위치 2:** `global/ai/tutor/AiTutorService.java` — 호출부에 현재 노트 ID 추가

```java
// 변경 전
ragRetrievalService.retrieve(note.getContent(), userId)

// 변경 후
ragRetrievalService.retrieve(note.getContent(), userId, note.getNoteId())
```

---

## 작업 우선순위

| 작업 | 중요도 | 이유 |
|------|--------|------|
| NoteService + embed() 연결 | 필수 | RAG 전체가 여기서 시작 |
| NoteController CRUD API | 필수 | 클라이언트 노트 저장 수단 없음 |
| RAG 컨텍스트 역할 명확화 (프롬프트 + 코드) | 높음 | RAG 활성화 시 현재 노트 대신 과거 노트 기반 질문 생성 위험 |
| 현재 노트 검색 결과 제외 필터 | 높음 | NoteService 구현 후 자기 자신이 similarity 1.0으로 검색됨 |
| 삭제 시 delete() 연동 | 높음 | Chroma 고아 벡터 누적 |
| Repository 쿼리 메서드 | 중간 | 목록 조회 필요 시 |

---

## 구현 순서 권장

```
1. StudyNoteRepository 쿼리 메서드 추가
        ↓
2. NoteService 구현 (embed/delete 연동 포함)
        ↓
3. NoteController + DTO 구현
        ↓
4. RagContext.format() 레이블 추가
   RagRetrievalService.retrieve() 시그니처 변경 + 현재 노트 제외 필터 추가
   AiTutorService 호출부 수정
        ↓
5. 통합 테스트: 노트 저장 → ChromaDB 확인 → AI 대화 시 RAG 결과 확인
```
