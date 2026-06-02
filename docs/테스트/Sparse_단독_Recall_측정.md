# Sparse 단독 Recall@4 측정

## 목적

Sparse 검색 단독 Recall@4를 측정하여 Dense-only(0.18) 및 하이브리드(0.43)와 비교한다.
Sparse가 Dense 없이 독립적으로 얼마나 관련 노트를 찾을 수 있는지 확인하는 것이 목적이다.

---

## 측정 모드 설정

`application.yml`에서 아래와 같이 설정하고 서버를 재시작한다.

```yaml
rag:
  similarity-threshold: 0.55
  bm25-max-results: 10   # Sparse 활성화
  dense-disabled: true   # Dense 비활성화 → Sparse-only 모드
```

---

## 측정 조건

- 노트 20개 ChromaDB 임베딩 (하이브리드 Recall@4 측정과 동일한 데이터셋)
- 쿼리 노트 10개 (카테고리 A: noteId=1,4,7,12,13 / 카테고리 B: noteId=2,3,8,10,11)
- 평가 지표: Recall@4

---

## 정답 연관 노트 (Ground Truth)

하이브리드 Recall@4 측정과 동일한 기준을 사용한다.

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

## 측정 실행

서버 실행 후 아래 명령어로 대화 시작 API를 10회 호출한다.

```bash
for i in 1 4 7 12 13 2 3 8 10 11; do
  curl -s -X POST http://localhost:8080/api/v1/conversations \
    -H "Content-Type: application/json" \
    -d "{\"noteId\": $i, \"type\": \"QUESTION\"}" | jq '.conversationId'
  sleep 0.5
done
```

---

## 로그 수집

```bash
grep "\[Recall측정\]" logs/application.log
```

출력 예시:
```
[Recall측정] [Sparse-only] 최종 Top-4: [스택, 큐, 연결리스트, ...]
```

---

## 결과 기록

| noteId | 쿼리 노트 | Sparse 건수 | Top-4 결과 | 정답 포함 수 | Recall@4 |
|---|---|---|---|---|---|
| 1 | HashMap | | | | |
| 4 | 이진 탐색 | | | | |
| 7 | 퀵정렬 | | | | |
| 12 | 연결 리스트 | | | | |
| 13 | 정렬 알고리즘 비교 | | | | |
| 2 | 스택 | | | | |
| 3 | 큐 | | | | |
| 8 | BFS | | | | |
| 10 | 힙 | | | | |
| 11 | 트리 순회 | | | | |
| **전체 평균** | | | | | |

---

## 측정 완료 후 복원

`application.yml`을 원래 설정으로 복원한다.

```yaml
rag:
  dense-disabled: false   # Dense 재활성화
```

---

## 비교 기준

| 검색 방식 | 전체 평균 Recall@4 |
|---|---|
| Dense-only | 0.18 |
| Sparse-only | (이번 측정) |
| 하이브리드 (Dense + Sparse + RRF) | 0.43 |
