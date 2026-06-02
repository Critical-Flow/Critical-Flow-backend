# REQ-AIV-010 — 관련 노트 검색 재현율 회귀 측정

## 목적

검색 설정(임계값, 검색 정책 등)이 변경될 때마다 하이브리드 검색의 Recall@4가
목표치 이상을 유지하는지 확인한다.

**목표치: 전체 평균 Recall@4 ≥ 0.35**

현재 기준선: 0.43 (2026-06-02 측정)

---

## 측정 트리거

아래 변경이 발생하면 이 문서의 절차에 따라 회귀 측정을 수행한다.

- `rag.similarity-threshold` 변경
- `rag.bm25-max-results` 변경
- `isTopicRelevant()` 키워드 추출 조건 변경 (한국어/영어 최소 길이, 최대 개수, 오버랩 임계값)
- 임베딩 모델 교체

---

## 데이터셋

하이브리드 Recall@4 측정(#62)과 동일한 데이터셋을 사용한다.

### 노트 20개 (noteId=1~20)

`docs/테스트/2차_필터_수동_측정_노트.md` 참조

### 쿼리 노트 10개 및 Ground Truth

| noteId | 쿼리 노트 | 정답 연관 노트 | 정답 수 |
|---|---|---|---|
| 1 | HashMap | 연결리스트, 트리순회, 시간복잡도 | 3 |
| 4 | 이진 탐색 | 퀵정렬, 정렬비교, 트리순회, 시간복잡도 | 4 |
| 7 | 퀵정렬 | 이진탐색, 정렬비교, 재귀, 시간복잡도 | 4 |
| 12 | 연결 리스트 | HashMap, 스택, 큐 | 3 |
| 13 | 정렬 알고리즘 비교 | 퀵정렬, 힙, 시간복잡도 | 3 |
| 2 | 스택 | 큐, DFS, 재귀, 연결리스트 | 4 |
| 3 | 큐 | 스택, BFS, 힙, 연결리스트 | 4 |
| 8 | BFS | 큐, DFS, 트리순회 | 3 |
| 10 | 힙 | 큐, 정렬비교, 트리순회, 시간복잡도 | 4 |
| 11 | 트리 순회 | BFS, DFS, 이진탐색, 힙 | 4 |

---

## 측정 절차

### 1단계 — 환경 준비

```bash
# 변경된 설정이 반영된 서버 실행 (환경변수 포함)
./gradlew bootRun
```

### 2단계 — 로깅 설정

`application.yml`에 파일 로깅 추가:

```yaml
logging:
  file:
    name: /tmp/criticalflow.log
```

### 3단계 — 노트 재임베딩

```bash
curl -s -X POST "http://localhost:8080/api/notes/reembed?userId=1"
# 응답: "재임베딩 완료: 20개"
```

### 4단계 — 쿼리 실행

```bash
for noteId in 1 4 7 12 13 2 3 8 10 11; do
  curl -s -X POST "http://localhost:8080/api/v1/conversations" \
    -H "Content-Type: application/json" \
    -d "{\"noteId\": $noteId, \"type\": \"QUESTION\", \"userId\": 1}"
  sleep 2
done
```

### 5단계 — 로그 수집

```bash
grep "\[Recall측정\]" /tmp/criticalflow.log | tail -20
```

### 6단계 — Recall@4 계산

각 쿼리 노트의 Top-4 결과에서 Ground Truth 정답 포함 수를 확인한다.

```
Recall@4(쿼리 노트) = 정답 포함 수 / 전체 정답 수
전체 평균 Recall@4 = Σ Recall@4(i) / 10
```

---

## 판정 기준

| 결과 | 판정 | 조치 |
|---|---|---|
| 전체 평균 Recall@4 ≥ 0.35 | **PASS** | 변경 사항 반영 |
| 전체 평균 Recall@4 < 0.35 | **FAIL** | 변경 사항 재검토 후 재측정 |

---

## 결과 기록 양식

측정일: `YYYY-MM-DD`  
변경 내용: `(무엇을 변경했는지 기록)`  
현재 설정:
- `rag.similarity-threshold`: 
- `rag.bm25-max-results`: 
- 키워드 최소 길이 (한국어/영어): 
- 오버랩 임계값: 

| noteId | 쿼리 노트 | Top-4 결과 | 정답 포함 수 | Recall@4 |
|---|---|---|---|---|
| 1 | HashMap | | | |
| 4 | 이진 탐색 | | | |
| 7 | 퀵정렬 | | | |
| 12 | 연결 리스트 | | | |
| 13 | 정렬 알고리즘 비교 | | | |
| 2 | 스택 | | | |
| 3 | 큐 | | | |
| 8 | BFS | | | |
| 10 | 힙 | | | |
| 11 | 트리 순회 | | | |
| **전체 평균** | | | | |

판정: **PASS / FAIL**

---

## 측정 이력

| 측정일 | 변경 내용 | Recall@4 | 판정 |
|---|---|---|---|
| 2026-06-02 | 기준선 (하이브리드 최초 측정, test/sparse-only-recall 환경) | 0.43 | PASS |
| 2026-06-02 | REQ-AIV-010 회귀 측정 최초 실행 (ChromaDB 호환성 이슈) | 0.21 | FAIL |
| 2026-06-02 | ChromaDB 호환성 수정 후 재측정 | **0.42** | **PASS ✅** |

### 2026-06-02 수정 내역 — ChromaDB 1.0.0 호환성

**문제**: `note_id != '{id}'` 필터가 ChromaDB 1.0.0에서 0건 반환
- `&&` 조합 필터에서 `!=` 연산자 비호환
- 기존 ChromaDB 컬렉션의 HNSW 인덱스 손상으로 similarity search 1건만 반환

**수정 내용**:
1. `note_id !=` 필터를 Java 단 후처리로 변경 (`RagRetrievalService.denseSearch`, `sparseSearch`)
2. ChromaDB 컬렉션 재생성 후 재임베딩 (cosine space 명시)
