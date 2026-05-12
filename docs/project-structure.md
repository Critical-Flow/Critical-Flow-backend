# 현재 프로젝트 구조

> 현재 `develop` 브랜치 기준 실제 파일 구조입니다.

---

## 디렉토리 구조

```
src/main/java/com/criticalflow/
│
├── CriticalFlowApplication.java
│
├── domain/
│   ├── conversation/
│   │   ├── controller/
│   │   │   └── ConversationController.java
│   │   ├── dto/
│   │   │   ├── ConversationResponse.java
│   │   │   ├── MessageResponse.java
│   │   │   ├── SendMessageRequest.java
│   │   │   └── StartConversationRequest.java
│   │   ├── entity/
│   │   │   ├── AiConversation.java               questionType 필드 포함
│   │   │   ├── AiMessage.java
│   │   │   └── QuestionType.java                 TYPE_A ~ TYPE_F enum
│   │   ├── repository/
│   │   │   ├── AiConversationRepository.java
│   │   │   └── AiMessageRepository.java
│   │   └── service/
│   │       └── ConversationService.java
│   ├── focus/
│   │   ├── entity/
│   │   │   └── FocusEvent.java
│   │   └── repository/
│   │       └── FocusEventRepository.java
│   ├── note/
│   │   ├── controller/
│   │   │   └── NoteController.java
│   │   ├── dto/
│   │   │   ├── NoteCreateRequest.java
│   │   │   ├── NoteUpdateRequest.java
│   │   │   └── NoteResponse.java
│   │   ├── entity/
│   │   │   └── StudyNote.java
│   │   ├── repository/
│   │   │   └── StudyNoteRepository.java
│   │   └── service/
│   │       └── NoteService.java
│   └── user/
│       ├── entity/
│       │   └── User.java
│       ├── repository/
│       │   └── UserRepository.java
│       └── service/
│           └── CustomOAuth2UserService.java
│
└── global/
    ├── ai/
    │   ├── advisor/
    │   │   ├── QuestionTypeAdvisor.java          CallAdvisor 구현 — TYPE 프롬프트 주입
    │   │   └── QuestionTypePromptProvider.java   TYPE별 프롬프트 텍스트 관리
    │   ├── rag/
    │   │   ├── FocusEventFormatter.java
    │   │   ├── NoteEmbeddingService.java         원본 마크다운 그대로 임베딩 (전처리 미사용, #57)
    │   │   ├── NoteMetadataExtractor.java        언어/헤더 자동 추출
    │   │   ├── NotePreprocessor.java             ※ 파일 존재하나 NoteEmbeddingService에서 미사용 (#57)
    │   │   ├── RagContext.java
    │   │   └── RagRetrievalService.java          excludeNoteId + Dense/Sparse 하이브리드 검색 + RRF
    │   ├── router/
    │   │   └── QuestionTypeRouter.java           규칙 필터(TYPE_E) + LLM 분류(gpt-4o)
    │   └── tutor/
    │       ├── AiTutorService.java
    │       ├── TutorRequest.java
    │       └── TutorResponse.java
    ├── auth/
    │   ├── OAuth2SuccessHandler.java
    │   └── jwt/
    │       ├── JwtAuthFilter.java
    │       └── JwtProvider.java
    ├── config/
    │   └── SecurityConfig.java
    ├── exception/
    │   ├── ErrorResponse.java
    │   └── GlobalExceptionHandler.java
    └── healthcheck/
        └── controller/HealthController.java

src/main/resources/
├── application.yml
└── prompts/
    ├── tutor-system.st                           {selected_question_type} 플레이스홀더 포함
    └── type-router.st                            LLM 질문 타입 분류 프롬프트
```

---

## 엔티티 스키마

### ai_conversation

| 컬럼 | 타입 | 설명 |
|------|------|------|
| conversation_id | BIGINT PK | |
| note_id | BIGINT | |
| user_id | BIGINT | |
| type | ENUM(QUESTION, QUIZ) | |
| question_type | VARCHAR(10) | TYPE_A ~ TYPE_F (라우팅 결정 결과 저장) |
| created_at | DATETIME | |

### ChromaDB Document 메타데이터

| 필드 | 값 예시 | 용도 |
|------|--------|------|
| `note_id` | `"42"` | 자기 참조 방지 필터 |
| `user_id` | `"7"` | 사용자 격리 필터 |
| `session_id` | `"15"` | RagContext 포맷 출력 |
| `title` | `"HashMap 정리"` | RagContext 포맷 출력 |
| `created_at` | `"2026-05-07T..."` | 향후 시간 기반 필터용 |
| `languages` | `"java,kotlin"` | 향후 언어 기반 필터용 |
| `headers` | `"스택,시간복잡도"` | 향후 헤더 기반 필터용 |

---

## 데이터 흐름

### 노트 저장 흐름

```
POST /api/notes
    │
    ▼
NoteController → NoteService.saveNote()
    │
    ├── StudyNote DB 저장
    └── NoteEmbeddingService.embed(note)
            │
            ├── vectorStore.delete("note-{noteId}")       ← 기존 벡터 먼저 삭제
            ├── NoteMetadataExtractor.extractLanguages()
            ├── NoteMetadataExtractor.extractHeaders()
            └── ChromaDB.add(Document)                    ← 원본 content 그대로 저장
```

> NotePreprocessor는 파일이 존재하지만 #57 측정 결과(전처리 적용 시 Recall@4 40%로 하락)에 따라 호출을 제거했다.

### AI 튜터 응답 흐름

```
POST /api/v1/conversations/{id}/messages
    │
    ▼
AiTutorService.respond()
    │
    ├── RagRetrievalService.retrieve(noteContent, userId, excludeNoteId)
    │       │
    │       ├── [1] Dense 검색
    │       │       VectorStore.similaritySearch(threshold=0.55, topK=6)
    │       │       filterExpression: user_id == userId && note_id != excludeNoteId
    │       │
    │       ├── [2] Sparse 검색
    │       │       extractKeyTerms() → 한국어 1자↑ / 영어 4자↑ 키워드 추출
    │       │       VectorStore.similaritySearch(threshold=0.0, topK=10)
    │       │       keywords.anyMatch(content::contains) → 키워드 포함 문서만 통과
    │       │
    │       ├── [3] RRF 병합
    │       │       score = Σ 1/(60 + rank)  (양쪽 등장 문서는 점수 합산)
    │       │       → 상위 4개 선별
    │       │
    │       ├── [4] 2차 키워드 필터 (isTopicRelevant)
    │       │       한국어 2자↑ / 영어 4자↑ 의미 키워드 중 10% 이상 일치해야 통과
    │       │
    │       └── RagContext 반환
    │
    ├── FocusEventFormatter.format(sessionId)
    │       FocusEventRepository 조회 (최근 15분 이내 집중 이탈 이벤트)
    │       → {focus_events} 문자열 생성
    │
    └── ChatClient 호출 (Advisor 체인)
            │
            ▼
        QuestionTypeAdvisor.adviseCall()
            │
            ├── QuestionTypeRouter.route(note, ragContext)
            │       ├── [규칙] 코드 블록 있음? → TYPE_E 즉시 반환 (LLM 호출 없음)
            │       └── [LLM] gpt-4o 분류 → TYPE_A~F 결정
            │               ├── TYPE_C + RAG 없음 → TYPE_A 폴백
            │               └── 매칭 실패 → TYPE_A 폴백 (기본값)
            │               └── AiConversation.questionType DB 저장
            │
            └── 선택된 TYPE 설명만 시스템 프롬프트에 주입
                    │
                    ▼
                GPT-4o 호출 → TutorResponse
```

### 세션 종료 흐름

```
questionCount >= MAX_QUESTIONS (summaryMode = true)
    │
    ▼
요약 응답 반환 후 이후 세션에서 질문 미생성
```

> SessionSummaryService(대화 요약 임베딩)는 미구현 상태다. 실사용 데이터 축적 후 도입 검토.

---

## 시스템 프롬프트 변수 바인딩

| 변수 | 소스 | 담당 코드 |
|------|------|----------|
| `{current_note}` | `StudyNote.content` | `AiTutorService` |
| `{rag_context}` | ChromaDB (Dense+Sparse+RRF) | `RagRetrievalService` → `RagContext.format()` |
| `{focus_events}` | `FocusEvent` DB | `FocusEventFormatter` |
| `{conversation_type}` | `AiConversation.type` | `AiTutorService` |
| `{question_count}` | `AiMessage` 카운트 | `AiTutorService` |
| `{has_code}` | 코드 블록 존재 여부 | `AiTutorService` |
| `{selected_question_type}` | 라우터 결정 TYPE | `QuestionTypeAdvisor` |
