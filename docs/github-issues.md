# GitHub 이슈 목록

> 아래 이슈를 순서대로 GitHub에 등록하세요.
> 의존성이 있는 이슈는 제목에 명시되어 있습니다.

---

## 이슈 #1 — 노트 CRUD API 기반 구현 (NoteService + NoteController)

---

## 어떤 기능인가요?

현재 `StudyNote` 엔티티와 `StudyNoteRepository`만 존재하며 서비스/컨트롤러가 없어 노트 저장·수정·삭제 API가 없습니다. 이후 모든 RAG 고도화 작업의 시작점이 되는 기반 레이어를 구현합니다.

## 작업 상세 내용

- [ ] `StudyNoteRepository`에 커스텀 쿼리 메서드 추가
  - `findByUserIdOrderByCreatedAtDesc(Long userId)`
  - `findBySessionId(Long sessionId)`
  - `findByNoteIdAndUserId(Long noteId, Long userId)` — 권한 검증용
- [ ] `NoteService.java` 신규 구현
  - `saveNote(...)` — `noteRepository.save()` 호출
  - `updateNote(...)` — `noteRepository.save()` 호출
  - `deleteNote(noteId)` — `noteRepository.delete()` 호출
- [ ] 노트 관련 DTO 클래스 생성 (`NoteCreateRequest`, `NoteUpdateRequest`, `NoteResponse`)
- [ ] `NoteController.java` 신규 구현

  | Method | URL | 설명 |
  |--------|-----|------|
  | `POST` | `/api/notes` | 노트 생성 |
  | `PUT` | `/api/notes/{noteId}` | 노트 수정 |
  | `DELETE` | `/api/notes/{noteId}` | 노트 삭제 |
  | `GET` | `/api/notes?userId={userId}` | 유저 노트 목록 조회 |

## 참고할만한 자료

- `docs/note-feature-todo.md` — 작업 목록 및 우선순위
- `docs/project-structure.md` — 현재 미구현 항목 목록

---

---

## 이슈 #2 — NoteEmbeddingService 연동 — embed/delete 트리거 연결 (이슈 #1 이후)

---

## 어떤 기능인가요?

`NoteEmbeddingService`가 구현되어 있지만 어디서도 호출되지 않아 ChromaDB가 비어있습니다. 노트 저장/삭제 시 자동으로 임베딩이 동기화되도록 연결하고, RAG 검색 시 현재 노트 자신이 검색 결과에 포함되는 문제를 제거합니다.

## 작업 상세 내용

- [ ] `NoteService.saveNote()` 내부에서 `noteEmbeddingService.embed(note)` 호출 추가
- [ ] `NoteService.updateNote()` 내부에서 `noteEmbeddingService.embed(note)` 호출 추가 (기존 벡터 자동 삭제 후 재삽입)
- [ ] `NoteService.deleteNote()` 내부에서 `noteEmbeddingService.delete(noteId)` 호출 추가
- [ ] `RagRetrievalService.retrieve()` 시그니처에 `excludeNoteId` 파라미터 추가
  ```java
  // 변경 전
  public RagContext retrieve(String queryText, Long userId)
  // 변경 후
  public RagContext retrieve(String queryText, Long userId, Long excludeNoteId)
  ```
- [ ] `RagRetrievalService` 필터 표현식에 현재 노트 제외 조건 추가
  ```java
  "user_id == '" + userId + "' && note_id != '" + excludeNoteId + "'"
  ```
- [ ] `AiTutorService`에서 `retrieve()` 호출 시 `note.getNoteId()` 전달하도록 수정

## 참고할만한 자료

- `docs/note-feature-todo.md` — 이슈 7번 항목 참고
- `docs/ai_current_content.md` — RAG 설계 결정 사항

---

---

## 이슈 #3 — RAG 품질 개선 — 한국어 필터 완화 + 레이블 추가 (이슈 #2 이후)

---

## 어떤 기능인가요?

RAG 검색 품질에 영향을 주는 두 가지 문제를 수정합니다.
1. 한국어 2~3자 핵심 개념어("스택", "큐", "재귀")가 현재 4자 초과 필터에 의해 탈락됩니다.
2. `{rag_context}`에 레이블이 없어 LLM이 과거 노트를 현재 노트와 동일하게 취급할 수 있습니다.

## 작업 상세 내용

- [ ] `RagRetrievalService.isTopicRelevant()` 한국어 감지 로직 추가
  ```java
  boolean hasKorean = queryText.chars().anyMatch(c -> c >= 0xAC00 && c <= 0xD7A3);
  int minLength = hasKorean ? 1 : 3;
  ```
- [ ] `RagContext.format()` 반환 문자열 앞에 레이블 추가
  ```java
  "[참고용 과거 노트 — 현재 노트 학습 보조 목적으로만 활용]\n\n" + ...
  ```
- [ ] `tutor-system.st` 프롬프트에 RAG 역할 명확화 규칙 추가
  - 질문은 반드시 `{current_note}` 기반으로 생성
  - `{rag_context}`는 TYPE C (Spaced Recall) 보조 목적으로만 참고

## 참고할만한 자료

- `docs/note-feature-todo.md` — 이슈 5, 6번 항목
- `docs/ai_current_content.md` — RAG 설계 결정 사항

---

---

## 이슈 #4 — 임베딩 전 노트 전처리 — NotePreprocessor 구현 (이슈 #2 이후)

---

## 어떤 기능인가요?

한국어 설명과 코드가 혼합된 노트를 그대로 임베딩하면 두 도메인의 평균 벡터가 생성되어 유사도 검색 정확도가 낮아집니다. 임베딩 전에 코드 블록을 의미 있는 식별자/주석 텍스트로 변환하는 전처리기를 구현합니다.

## 작업 상세 내용

- [ ] `NotePreprocessor.java` 신규 구현
  - 코드 블록 정규식 추출: `` Pattern.compile("```(\\w+)?\\n([\\s\\S]*?)```") ``
  - 코드 블록 → `[java: factorial recursive baseCase]` 형태로 변환
  - `extractIdentifiers()` — 주석 + camelCase/snake_case 식별자 추출 (3자 초과)
- [ ] `NoteEmbeddingService.embed()` 내부에서 임베딩 전 `preprocessor.preprocessForEmbedding(note.getContent())` 호출
- [ ] 전처리 전/후 벡터 비교 테스트 케이스 작성

## 참고할만한 자료

- `docs/ai-enhancement-plan.md` — 1번 항목 (한국어 + 코드 혼합 노트 전처리)

---

---

## 이슈 #5 — 노트 메타데이터 자동 추출 — 언어 + 헤더 (이슈 #4 이후)

---

## 어떤 기능인가요?

현재 ChromaDB에 저장되는 메타데이터에 프로그래밍 언어와 마크다운 헤더 정보가 없어, "Java 재귀 노트"와 "Python 재귀 노트"를 구분할 수 없습니다. 노트 저장 시 자동으로 언어와 헤더를 추출해 메타데이터에 포함합니다.

## 작업 상세 내용

- [ ] `NoteMetadataExtractor.java` 신규 구현
  - `extractLanguages(String markdown)` — 코드 펜스 언어 추출
    ```java
    Pattern.compile("```(\\w+)")  // ```java → "java", ```python → "python"
    ```
  - `extractHeaders(String markdown)` — `#` 헤더 텍스트 추출
- [ ] `NoteEmbeddingService.embed()` 메타데이터 맵에 `languages`, `headers` 필드 추가
  ```java
  "languages", String.join(",", extractor.extractLanguages(note.getContent()))
  "headers",   String.join(",", extractor.extractHeaders(note.getContent()))
  ```
- [ ] 언어 미지정 코드 블록(``` `` ` ``` 만 있는 경우) 처리 — `"unknown"` 으로 저장
- [ ] (선택) RAG 검색 시 같은 언어 필터 적용 옵션 추가

## 참고할만한 자료

- `docs/ai-enhancement-plan.md` — 2번 항목 (메타데이터 자동 추출)
- 언어 자동 추출 vs 사용자 입력 논의: 코드 펜스 자동 추출 + 유저 프로필 기본 언어 폴백 방식으로 결정

---

---

## 이슈 #6 — 시스템 프롬프트 TYPE E/F 추가

---

## 어떤 기능인가요?

현재 시스템 프롬프트에는 TYPE A~D만 존재합니다. 코드가 포함된 노트 전용 질문 전략(TYPE E)과 순수 텍스트 이론 노트 전용 질문 전략(TYPE F)을 추가합니다.

## 작업 상세 내용

- [ ] `tutor-system.st`에 TYPE E 추가
  ```
  TYPE E — Code Behavior Probe
    When: the note contains a code block (```language ... ```)
    Goal: check whether the learner understands what the code actually does
    Template options:
      - "이 코드의 시간복잡도는 어떻게 되나요? 왜 그렇게 생각하나요?"
      - "이 코드에서 [특정 라인]이 없다면 어떤 일이 생길까요?"
      - "이 함수에 [엣지케이스 입력]을 넣으면 어떻게 동작할까요?"
    Constraint: 절대 올바른 코드를 제시하거나 수정해주지 말 것 (LAW 1)
  ```
- [ ] `tutor-system.st`에 TYPE F 추가
  ```
  TYPE F — Contextual Application Probe
    When: the note contains no code blocks and is a purely conceptual/theoretical note
    Goal: check whether the learner can apply the concept to a real situation
    Template options:
      - "[개념]이 실제로 어떤 문제를 해결하기 위해 등장했나요?"
      - "[개념 A]와 [개념 B]는 어떤 상황에서 각각 선택하나요?"
  ```
- [ ] `AiTutorService.resolvePrompt()`에 `{has_code}` 변수 바인딩 추가
  ```java
  boolean hasCodeBlock = note.getContent().contains("```");
  .replace("{has_code}", String.valueOf(hasCodeBlock))
  ```
- [ ] `tutor-system.st`에 `{has_code}` 플레이스홀더 추가 및 TYPE E/F 활성화 조건 명시

## 참고할만한 자료

- `docs/ai-enhancement-plan.md` — 4번 항목 (TYPE E), 6-3번 항목 (TYPE F)

---

---

## 이슈 #7 — AiConversation questionType 필드 추가 + 학습 데이터 축적 인프라

---

## 어떤 기능인가요?

LLM이 결정한 질문 TYPE(A~F)을 DB에 저장해 향후 OpenAI Fine-tuning용 학습 데이터를 점진적으로 축적하는 인프라를 구축합니다. 전체 누적 200~300개 이후 Fine-tuning 진행 예정입니다.

## 작업 상세 내용

- [ ] `QuestionType` enum 정의 (`TYPE_A` ~ `TYPE_F`)
- [ ] `AiConversation.java`에 `questionType` 필드 추가
  ```java
  @Enumerated(EnumType.STRING)
  @Column(name = "question_type")
  private QuestionType questionType;
  ```
- [ ] DB 마이그레이션 실행
  ```sql
  ALTER TABLE ai_conversation ADD COLUMN question_type VARCHAR(10);
  ```
- [ ] `AiConversationRepository`에 Fine-tuning 데이터 추출 쿼리 추가
  ```java
  @Query("SELECT c.questionType, n.content FROM AiConversation c JOIN StudyNote n ON c.noteId = n.noteId WHERE c.questionType IS NOT NULL")
  List<Object[]> findTrainingData();
  ```
- [ ] `ConversationService.start()`에 `questionType` 파라미터 추가 및 저장 처리

## 참고할만한 자료

- `docs/question-type-routing.md` — 최종 결정: 3단계 점진적 전환
- `docs/ai-enhancement-plan.md` — 8번 항목 (질문 타입 학습 데이터 축적)

---

---

## 이슈 #8 — 질문 타입 LLM 라우터 구현 — QuestionTypeRouter (이슈 #6, #7 이후)

---

## 어떤 기능인가요?

노트 내용을 분석해 TYPE A~F 중 가장 적합한 질문 유형을 결정하는 라우터를 구현합니다. 규칙 기반 전처리 필터(코드 블록 감지 → TYPE E 즉시 분기)와 LLM 분류(GPT-4o-mini few-shot)를 조합합니다.

## 작업 상세 내용

- [ ] `QuestionTypeRouter.java` 신규 구현
  - 1단계 — 규칙 기반 필터: 코드 블록 존재 시 TYPE E 즉시 반환
  - 2단계 — LLM 분류: GPT-4o-mini 호출, TYPE A~F 정의 + 5개 few-shot 예시 포함
  - few-shot 예시 설계 (각 TYPE당 1개, 한국어 CS 노트 기준)
- [ ] LLM 분류 프롬프트 템플릿 작성 (`type-router.st` 신규 파일)
  ```
  노트 내용을 보고 TYPE_A ~ TYPE_F 중 하나만 답하라.
  [TYPE 정의]
  [예시 5개]
  노트: {note_content}
  답:
  ```
- [ ] `QuestionTypeRouter`에서 결정된 TYPE을 `AiConversation.questionType`에 저장
- [ ] TYPE C는 `RagContext.isEmpty()` 일 때 후보에서 제외하는 로직 추가

## 참고할만한 자료

- `docs/question-type-routing.md` — 최종 결정 1단계
- `docs/ai-enhancement-plan.md` — 7번 항목 라우팅 구조 다이어그램

---

---

## 이슈 #9 — Custom Advisor 구조 도입 — QuestionTypeAdvisor (이슈 #8 이후)

---

## 어떤 기능인가요?

현재 `AiTutorService`는 `ChatModel`을 직접 호출합니다. `ChatClient` + `CallAroundAdvisor` 구조로 전환해 LLM 호출 직전에 선택된 TYPE의 프롬프트만 주입하도록 합니다. 매 턴 약 150 토큰 절감 효과가 있습니다.

## 작업 상세 내용

- [ ] `AiTutorService`를 `ChatModel` 직접 호출 → `ChatClient` 방식으로 전환
- [ ] `QuestionTypeAdvisor.java` 신규 구현 (`CallAroundAdvisor` 구현)
  - `aroundCall()` 내부에서 `QuestionTypeRouter.route()` 호출
  - 선택된 TYPE 설명만 시스템 프롬프트에 주입
- [ ] `QuestionTypePromptProvider.java` 신규 구현 — TYPE별 프롬프트 텍스트 관리
- [ ] `tutor-system.st` 수정
  - TYPE A~F 전체 설명 제거 (~200 토큰)
  - `{selected_question_type}` 플레이스홀더로 교체 (~50 토큰)
- [ ] `ChatClient` Advisor 체인에 컨텍스트 주입 방식으로 변경
  ```java
  chatClient.prompt()
      .advisors(advisor -> advisor
          .param("note", note)
          .param("ragContext", ragContext)
          .param("questionCount", questionCount))
      .call();
  ```

## 참고할만한 자료

- `docs/ai-enhancement-plan.md` — 7번 항목 전체
- Spring AI `CallAroundAdvisor` 공식 문서

---

---

## 이슈 #10 — 하이브리드 검색 — Dense + BM25 (이슈 #2 이후)

---

## 어떤 기능인가요?

현재 Dense vector 검색만 사용해 코드 식별자/함수명 정확 매칭에 약점이 있습니다. BM25 스파스 검색을 병렬로 수행하고 RRF(Reciprocal Rank Fusion)로 결합해 검색 정확도를 높입니다.

## 작업 상세 내용

- [ ] `RagRetrievalService`에 BM25 스파스 검색 추가
  - 쿼리에서 키워드 추출 (`extractKeyTerms()` — 4자 이상 유의미한 토큰)
  - ChromaDB FTS 인덱스(`#document`) 활용
- [ ] RRF 병합 로직 구현 (`mergeWithRRF()`)
  ```java
  // k=60 관례값, 두 순위 리스트의 1/(k+rank) 점수 합산
  scores.merge(id, 1.0 / (k + i + 1), Double::sum);
  ```
- [ ] Dense / Sparse 각각 `topK` 파라미터 분리 설정
- [ ] BM25 검색에는 similarity threshold 미적용 (키워드 매칭 방식이므로)
- [ ] `application.yml`에 BM25 관련 설정값 추가 (`rag.bm25-max-results`)

## 참고할만한 자료

- `docs/ai-enhancement-plan.md` — 3번 항목 (하이브리드 검색)

---

---

## 이슈 #11 — 세션 종료 시 요약 임베딩 — 장기 학습 패턴 추적 (이슈 #2 이후)

---

## 어떤 기능인가요?

개별 노트만 임베딩되어 있어 "이 학습자가 특정 개념을 얼마나 어려워했는가" 같은 학습 패턴 정보가 RAG에 없습니다. 세션 종료 시 대화 내용을 LLM으로 3줄 요약 후 ChromaDB에 별도 Document로 저장합니다.

## 작업 상세 내용

- [ ] `SessionSummaryService.java` 신규 구현
  - 세션 메시지 전체 조회 후 LLM 요약 생성
  - 요약 형식: `[학습 주제] | [이해한 부분] | [불명확한 부분]`
  - 요약 Document를 `type: "session_summary"` 메타데이터와 함께 ChromaDB 저장
- [ ] 세션 종료 트리거 연결
  - `questionCount >= MAX_QUESTIONS` (summaryMode 진입 시) 자동 호출
  - 또는 별도 세션 종료 API 엔드포인트 제공
- [ ] `RagRetrievalService.retrieve()`에서 `type == "session_summary"` 문서도 검색 대상에 포함
- [ ] `RagContext.format()`에서 session_summary 타입을 노트와 구분해 표시
  ```
  --- [Session Summary: "재귀 함수" | 2024-01-15] ---
  ```
- [ ] `tutor-system.st`에 session_summary 컨텍스트 활용 가이드 추가

## 참고할만한 자료

- `docs/ai-enhancement-plan.md` — 5번 항목 (세션 요약 임베딩)

---

## 이슈 우선순위 및 의존 관계

```
이슈 #1 (NoteService/Controller)
    └── 이슈 #2 (embed 연동)
            ├── 이슈 #3 (RAG 품질 개선)
            ├── 이슈 #4 (NotePreprocessor)
            │       └── 이슈 #5 (메타데이터 추출)
            ├── 이슈 #10 (하이브리드 BM25)
            └── 이슈 #11 (세션 요약 임베딩)

이슈 #6 (TYPE E/F 추가) ─────┐
이슈 #7 (questionType 필드) ──┼── 이슈 #8 (LLM 라우터)
                              │       └── 이슈 #9 (Custom Advisor)
```

| 이슈 | 우선순위 | 의존 이슈 |
|------|---------|---------|
| #1 NoteService/Controller | 필수 | 없음 |
| #2 embed 연동 | 필수 | #1 |
| #3 RAG 품질 개선 | 높음 | #2 |
| #4 NotePreprocessor | 높음 | #2 |
| #6 TYPE E/F 추가 | 높음 | 없음 |
| #7 questionType 필드 | 높음 | 없음 |
| #5 메타데이터 추출 | 중간 | #4 |
| #8 LLM 라우터 | 중간 | #6, #7 |
| #9 Custom Advisor | 중간 | #8 |
| #10 하이브리드 BM25 | 중간 | #2 |
| #11 세션 요약 임베딩 | 낮음 | #2 |
