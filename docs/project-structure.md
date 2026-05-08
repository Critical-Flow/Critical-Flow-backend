# 고도화 완료 후 프로젝트 구조

> 현재 구조 기준으로 이슈 #1 ~ #11 완료 후의 목표 파일 구조입니다.
> 🆕 = 신규 파일, ✏️ = 기존 파일 수정

---

## 디렉토리 구조

```
src/main/java/com/criticalflow/
│
├── CriticalFlowApplication.java
│
    ├── conversation/
│   ├── controller/
│   │   └── ConversationController.java           ✏️ questionType 파라미터 추가
│   ├── dto/
│   │   ├── ConversationResponse.java
│   │   ├── MessageResponse.java
│   │   ├── SendMessageRequest.java
│   │   └── StartConversationRequest.java         ✏️ questionType 필드 추가
│   └── service/
│       └── ConversationService.java              ✏️ questionType 저장 로직 추가
│
├── domain/
│   ├── ai/
│   │   ├── entity/
│   │   │   ├── AiConversation.java               ✏️ questionType 필드 추가
│   │   │   └── AiMessage.java
│   │   └── repository/
│   │       ├── AiConversationRepository.java     ✏️ fine-tuning 데이터 추출 쿼리 추가
│   │       └── AiMessageRepository.java
│   ├── focus/
│   │   ├── entity/
│   │   │   └── FocusEvent.java
│   │   └── repository/
│   │       └── FocusEventRepository.java
│   ├── note/
│   │   ├── controller/
│   │   │   └── NoteController.java               🆕 CRUD API (POST/PUT/DELETE/GET)
│   │   ├── dto/
│   │   │   ├── NoteCreateRequest.java            🆕
│   │   │   ├── NoteUpdateRequest.java            🆕
│   │   │   └── NoteResponse.java                 🆕
│   │   ├── entity/
│   │   │   └── StudyNote.java
│   │   ├── repository/
│   │   │   └── StudyNoteRepository.java          ✏️ 커스텀 쿼리 메서드 추가
│   │   └── service/
│   │       └── NoteService.java                  🆕 save/update/delete + embed 연동
│   └── user/
│       ├── entity/
│       │   └── User.java
│       └── repository/
│           └── UserRepository.java
│
└── global/
    ├── ai/
    │   ├── advisor/
    │   │   ├── QuestionTypeAdvisor.java          🆕 CallAroundAdvisor — TYPE 주입
    │   │   └── QuestionTypePromptProvider.java   🆕 TYPE별 프롬프트 텍스트 관리
    │   ├── enums/
    │   │   └── QuestionType.java                 🆕 TYPE_A ~ TYPE_F enum
    │   ├── rag/
    │   │   ├── FocusEventFormatter.java
    │   │   ├── NoteEmbeddingService.java         ✏️ 전처리 + 메타데이터 추출 적용
    │   │   ├── NoteMetadataExtractor.java        🆕 언어/헤더 자동 추출
    │   │   ├── NotePreprocessor.java             🆕 코드 블록 → 식별자 변환
    │   │   ├── RagContext.java                   ✏️ format()에 "[참고용 과거 노트]" 레이블 추가
    │   │   ├── RagRetrievalService.java          ✏️ excludeNoteId 파라미터 + BM25 병렬 검색 + 한국어 필터
    │   │   └── SessionSummaryService.java        🆕 세션 요약 생성 + ChromaDB 저장
    │   ├── router/
    │   │   └── QuestionTypeRouter.java           🆕 규칙 필터 + LLM 분류 (GPT-4o-mini)
    │   └── tutor/
    │       ├── AiTutorService.java               ✏️ ChatClient 전환 + has_code 변수 바인딩
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
├── application.yml                               ✏️ BM25 설정값 추가
└── prompts/
    ├── tutor-system.st                           ✏️ TYPE E/F 추가, {selected_question_type} 플레이스홀더
    └── type-router.st                            🆕 LLM 질문 타입 분류 프롬프트
```

---

## 고도화 후 엔티티 스키마 변경

### ai_conversation (변경)

| 컬럼 | 타입 | 설명 | 변경 |
|------|------|------|------|
| conversation_id | BIGINT PK | | |
| note_id | BIGINT | | |
| user_id | BIGINT | | |
| type | ENUM(QUESTION, QUIZ) | | |
| question_type | VARCHAR(10) | TYPE_A ~ TYPE_F | 🆕 추가 |
| created_at | DATETIME | | |

### ChromaDB Document 메타데이터 (변경)

| 필드 | 변경 전 | 변경 후 |
|------|---------|---------|
| note_id | ✅ | ✅ |
| user_id | ✅ | ✅ |
| session_id | ✅ | ✅ |
| title | ✅ | ✅ |
| created_at | ✅ | ✅ |
| languages | ❌ | 🆕 "java,kotlin" |
| headers | ❌ | 🆕 "스택,시간복잡도" |
| type | ❌ | 🆕 "note" or "session_summary" |

---

## 고도화 후 데이터 흐름

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
            ├── NotePreprocessor.preprocessForEmbedding()
            │       └── 코드 블록 → 식별자 변환
            ├── NoteMetadataExtractor.extractLanguages()
            ├── NoteMetadataExtractor.extractHeaders()
            └── ChromaDB.add(Document)
```

### AI 튜터 응답 흐름

```
POST /api/v1/conversations/{id}/messages
    │
    ▼
AiTutorService.respond()
    │
    ├── RagRetrievalService.retrieve(noteContent, userId, excludeNoteId)
    │       ├── Dense 검색 (기존)
    │       ├── BM25 스파스 검색 (신규)
    │       └── RRF 병합 → 한국어 키워드 필터 → RagContext
    │
    ├── FocusEventFormatter.format(sessionId)
    │
    └── ChatClient 호출 (Advisor 체인)
            │
            ▼
        QuestionTypeAdvisor.aroundCall()
            │
            ├── QuestionTypeRouter.route(note, ragContext)
            │       ├── [규칙] 코드 블록 있음? → TYPE_E 즉시 반환
            │       └── [LLM] GPT-4o-mini few-shot 분류 → TYPE 결정
            │               └── AiConversation.questionType 저장
            │
            └── 선택된 TYPE 설명만 시스템 프롬프트에 주입
                    │
                    ▼
                GPT-4o 호출 → TutorResponse
```

### 세션 종료 흐름

```
questionCount >= MAX_QUESTIONS (summaryMode)
    │
    ▼
SessionSummaryService.summarizeAndEmbed(conversationId)
    ├── AiMessage 전체 조회
    ├── LLM 요약 생성: "[학습 주제] | [이해] | [불명확]"
    └── ChromaDB.add(Document, type="session_summary")
```

---

## 고도화 후 시스템 프롬프트 변수 바인딩

| 변수 | 소스 | 담당 코드 |
|------|------|----------|
| `{current_note}` | `StudyNote.content` | `AiTutorService` |
| `{rag_context}` | ChromaDB (Dense+BM25) | `RagRetrievalService` → `RagContext.format()` |
| `{focus_events}` | `FocusEvent` DB | `FocusEventFormatter` |
| `{conversation_type}` | `AiConversation.type` | `AiTutorService` |
| `{question_count}` | `AiMessage` 카운트 | `AiTutorService` |
| `{has_code}` | 코드 블록 존재 여부 | `AiTutorService` 🆕 |
| `{selected_question_type}` | 라우터 결정 TYPE | `QuestionTypeAdvisor` 🆕 |
