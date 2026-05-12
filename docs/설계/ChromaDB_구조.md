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
| `#document` (노트 본문) | FTS (Full-Text Search) | Sparse 검색 후보 수집 |
| `#embedding` (벡터) | Vector Index (HNSW + cosine) | Dense 유사도 검색 |
| 메타데이터 string 필드 | Inverted Index | `user_id`, `note_id` 등 필터 쿼리 |
| 메타데이터 int/float 필드 | Inverted Index | 범위 필터 |

---

## Document 구조

| 항목 | 내용 |
|------|------|
| Document ID | `"note-{noteId}"` |
| content | 노트 원본 마크다운 텍스트 (전처리 없음) |
| 청킹 | 없음 — 노트 전체가 하나의 Document |

**메타데이터**

| 키 | 값 예시 | 용도 |
|----|--------|------|
| `note_id` | `"42"` | 자기 자신 제외 필터 (`excludeNoteId`) |
| `user_id` | `"7"` | 사용자 격리 필터 |
| `session_id` | `"15"` | RagContext 포맷 출력 |
| `title` | `"HashMap 정리"` | RagContext 포맷 출력 |
| `created_at` | `"2026-05-07T..."` | 향후 시간 기반 필터용 |
| `languages` | `"python,java"` | 향후 언어 기반 필터용 |
| `headers` | `"개요,구현,정리"` | 향후 헤더 기반 필터용 |

> `content`는 #57 측정 결과에 따라 전처리 없이 원본 그대로 저장한다. 코드 블록을 식별자 목록으로 변환했을 때 Recall@4가 100% → 40%로 하락하는 것을 확인했기 때문이다.

---

## 임베딩 저장 흐름

```
NoteService.saveNote() / updateNote()
    │
    ▼
NoteEmbeddingService.embed(StudyNote)
    ├── vectorStore.delete("note-{noteId}")     ← 기존 벡터 먼저 삭제 (버전 일관성)
    ├── NoteMetadataExtractor.extractLanguages() ← languages 메타데이터 추출
    ├── NoteMetadataExtractor.extractHeaders()   ← headers 메타데이터 추출
    └── vectorStore.add(Document)               ← 원본 content + 메타데이터 저장

NoteService.deleteNote()
    │
    ▼
NoteEmbeddingService.delete(noteId)
    └── vectorStore.delete("note-{noteId}")
```

---

## Q&A

### Q. 컬렉션 설정(ef_construction 등)을 직접 ChromaDB에 접속해서 설정한 것인가?

아니다. `application.yml`의 `initialize-schema: true` 설정으로 앱 시작 시 Spring AI가 자동으로 컬렉션을 생성한다. `ef_construction`, `ef_search`, `max_neighbors`는 Spring AI ChromaDB VectorStore 구현체의 기본값(100, 100, 16)이 그대로 적용된다.

컬렉션명(`criticalflow-notes`)만 직접 지정했고, 나머지는 기본값을 사용한 것이다.

---

### Q. 유사도 공간(cosine)도 자동으로 설정되는 것인가?

그렇다. Spring AI의 ChromaDB VectorStore가 컬렉션 생성 시 cosine으로 고정해서 만든다. 별도로 지정하는 코드는 없다.

cosine을 사용하는 이유는 텍스트 임베딩에서 의미 유사도를 측정할 때 벡터의 크기가 아닌 **방향(의미)**을 기준으로 비교하는 것이 더 정확하기 때문이다. 노트의 길이에 관계없이 내용의 유사도를 측정할 수 있다.

---

### Q. 인덱스 자동 생성 필드(`#document`, `#embedding`, 메타데이터)는 어떻게 한 번에 만들어지는가?

`vectorStore.add(document)` 한 줄 호출 시 내부에서 3단계가 자동으로 처리된다.

```
vectorStore.add(document)
    ├── 1. OpenAI API → content를 벡터(float[1536])로 변환 → #embedding 인덱스 생성
    ├── 2. content 텍스트 → ChromaDB FTS 인덱스 → #document 인덱스 생성
    └── 3. Map.of(...) 메타데이터 → 각 string 필드마다 Inverted Index 생성
```

코드에서는 `vectorStore.add()`만 호출하면 ChromaDB와 Spring AI가 나머지를 처리한다.

---

### Q. 메타데이터는 자동으로 생성되는 것인가, 직접 설정하는 것인가?

**직접 설정**한다. `NoteEmbeddingService.embed()`에서 `Map.of()`로 키와 값을 명시적으로 정의한다. ChromaDB는 넘겨받은 Map을 그대로 저장할 뿐이다.

`#embedding`(벡터)과 `#document`(FTS)는 ChromaDB가 자동 생성하지만, 메타데이터는 개발자가 직접 정의한다.

---

### Q. `headers` 메타데이터는 어떤 정보를 저장하는가?

마크다운의 `#` 기호로 시작하는 줄을 파싱해 헤더 텍스트만 추출한다. `#`의 개수(레벨)는 구분하지 않고 모두 동일하게 처리한다.

예시:
```markdown
# 스택(Stack) 자료구조
## 정의
## 시간복잡도
```
→ `headers: "스택(Stack) 자료구조,정의,시간복잡도"`

현재는 저장만 해두고 검색 필터로 활용하지 않는다. 향후 "특정 헤더가 포함된 노트만 검색" 기능 확장을 위한 사전 준비다.

---

### Q. 메타데이터 int/float 필드 Inverted Index는 현재 사용하는가?

사용하지 않는다. 현재 모든 메타데이터는 `note.getNoteId().toString()`처럼 문자열로 변환해서 저장한다. int/float 타입 필드가 없으므로 이 인덱스는 생성되지 않는다.

해당 행은 ChromaDB의 기능 설명 차원에서 작성한 것이며, 향후 숫자 타입 메타데이터(예: 학습 점수, 난이도)를 추가할 경우 범위 필터(`score >= 3.0`)로 활용할 수 있다.

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
