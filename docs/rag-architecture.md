# RAG 파이프라인 아키텍처

## RAG 2단계 필터링

```
1차 필터 — Chroma similarity threshold (기본값: 0.75)
    ↓
2차 필터 — isTopicRelevant()
    • 쿼리의 의미 있는 키워드(4자 초과) 추출
    • 해당 키워드 중 20% 이상이 Document 내용에 포함되어야 통과
    • 목적: similarity를 간신히 넘긴 토픽 불일치 노이즈 제거
    ↓
최대 4개 반환 (max-results)
```

---

## RAG 설계 결정 사항

| 항목 | 결정 | 이유 |
|------|------|------|
| 질문 메시지 임베딩 | 하지 않음 | 대화 히스토리는 DB 직접 로드, RAG 노이즈 방지 |
| 청킹 | 하지 않음 | 쿼리가 노트 전체 → 노트 간 토픽 유사도 비교 구조 |
| 임베딩 범위 | 노트 단위 독립 저장 | user_id 필터로 유저 간 데이터 격리 |

자세한 설계 근거 → [rag-design-decisions.md](rag-design-decisions.md)

---

## 관련 파일

| 역할 | 파일 경로 |
|------|-----------|
| 임베딩 저장/삭제 | `global/ai/rag/NoteEmbeddingService.java` |
| RAG 검색 | `global/ai/rag/RagRetrievalService.java` |
| 검색 결과 포맷 | `global/ai/rag/RagContext.java` |
| 집중 이벤트 포맷 | `global/ai/rag/FocusEventFormatter.java` |
| AI 튜터 오케스트레이션 | `global/ai/tutor/AiTutorService.java` |
| 시스템 프롬프트 템플릿 | `resources/prompts/tutor-system.st` |
