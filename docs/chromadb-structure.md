# ChromaDB 구조

> ChromaDB 1.0.0부터 v1 API가 deprecated되었고 **v2 API**를 사용한다.
> Spring AI 1.0.6으로 업그레이드하여 호환성 문제 해결됨.

---

## 계층 구조

```
default_tenant
    └── default_database  (id: 00000000-0000-0000-0000-000000000000)
            └── criticalflow-notes  (id: a349410b-8c99-4c09-93cc-356473a93888)
```

---

## 컬렉션 설정

| 항목 | 값 |
|------|-----|
| 컬렉션명 | `criticalflow-notes` |
| 유사도 공간 | **cosine** |
| 인덱스 알고리즘 | HNSW (Hierarchical Navigable Small World) |
| ef_construction | 100 |
| ef_search | 100 |
| max_neighbors | 16 |

---

## HNSW 파라미터 설명

| 파라미터 | 설명 |
|----------|------|
| `ef_construction` | 인덱스 구축 시 탐색 범위 — 높을수록 정확하지만 구축 느림 |
| `ef_search` | 검색 시 탐색 범위 — 높을수록 정확하지만 검색 느림 |
| `max_neighbors` | 각 노드가 연결하는 최대 이웃 수 (그래프 밀도) |

학습 노트 수백 건 규모에서는 현재 기본값으로 충분. 수십만 건 이상이 되면 조정 검토 필요.

---

## 인덱스 자동 생성 필드

| 필드 | 인덱스 종류 | 용도 |
|------|------------|------|
| `#document` (노트 본문) | FTS (Full-Text Search) | 전문 검색 |
| `#embedding` (벡터) | Vector Index (HNSW + cosine) | 유사도 검색 |
| 메타데이터 string 필드 | Inverted Index | `user_id` 등 필터 쿼리 |
| 메타데이터 int/float 필드 | Inverted Index | 범위 필터 |

---

## Document 구조

| 항목 | 내용 |
|------|------|
| Document ID | `"note-{noteId}"` |
| content | 노트 전체 마크다운 텍스트 |
| 메타데이터 | `note_id`, `session_id`, `user_id`, `title`, `created_at` |
| 청킹 | 없음 — 노트 전체가 하나의 Document |

---

## 현재 상태

```
컬렉션 document 수: 0
→ NoteEmbeddingService.embed()가 호출된 적 없음
→ NoteService / NoteController 미구현으로 인한 트리거 부재
```

---

## Docker 실행 설정 (application.yml)

```yaml
spring:
  ai:
    vectorstore:
      chroma:
        client:
          host: ${CHROMA_HOST:http://localhost}
          port: ${CHROMA_PORT:8000}
        tenant-name: ${CHROMA_TENANT:default_tenant}
        database-name: ${CHROMA_DATABASE:default_database}
        collection-name: criticalflow-notes
        initialize-schema: true
```
