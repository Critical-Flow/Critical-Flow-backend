# similarity-threshold 최적값 검증 가이드 (#60)

## 핵심 전략

서버를 5번 재시작하는 대신, **threshold=0.50으로 전체 후보를 한 번에 조회**하여
각 임계값(0.60~0.85)에서의 통과 여부를 로그 한 줄로 확인한다.

---

## 1단계 — 서버 실행 및 노트 확인

노트 20개가 이미 ChromaDB에 임베딩된 상태여야 한다 (#59 작업에서 완료).

```bash
./gradlew bootRun
```

---

## 2단계 — 대화 시작 API 10회 호출

```bash
for i in {1..10}; do
  curl -s -X POST http://localhost:8080/api/v1/conversations \
    -H "Content-Type: application/json" \
    -d "{\"noteId\": $i, \"type\": \"QUESTION\"}"
  sleep 0.5
done
```

각 호출마다 아래 형태의 로그가 출력된다:

```
[#60 THRESHOLD] ── 쿼리: 'HashMap은 Java에서 키-값 쌍...' ──
[#60 THRESHOLD] 노트 제목             | score  | 0.60 | 0.70 | 0.75 | 0.80 | 0.85
[#60 THRESHOLD] 스택(Stack) 자료구조   | 0.8234 |  O   |  O   |  O   |  O   |  X
[#60 THRESHOLD] 연결 리스트(Linked..  | 0.7891 |  O   |  O   |  O   |  X   |  X
[#60 THRESHOLD] 큐(Queue) 자료구조    | 0.7102 |  O   |  O   |  X   |  X   |  X
[#60 THRESHOLD] 이진 탐색(Binary...   | 0.6234 |  O   |  X   |  X   |  X   |  X
[#60 THRESHOLD] 총 4건 (threshold=0.50 기준)
```

---

## 3단계 — 결과 기록표

로그를 보면서 각 노트별로 아래 표를 채운다.

### Ground Truth 정의 방법
- 현재 노트(쿼리)와 **실제로 연관된** 과거 노트를 **수동으로 판단**한다.
- 예: HashMap 쿼리 → 연결 리스트, 트리, 시간복잡도가 관련 있음 → ✅로 표시

### 결과 기록 양식

> 아래 표를 로그 출력을 보며 채우세요.

#### noteId=1 (HashMap)
| 후보 노트 | score | Ground Truth | 0.60 | 0.70 | 0.75 | 0.80 | 0.85 |
|---------|-------|-------------|------|------|------|------|------|
| (노트명) | 0.XX | ✅/❌ | O/X | O/X | O/X | O/X | O/X |
| ... | | | | | | | |

#### noteId=2 (스택)
| 후보 노트 | score | Ground Truth | 0.60 | 0.70 | 0.75 | 0.80 | 0.85 |
|---------|-------|-------------|------|------|------|------|------|
| ... | | | | | | | |

*(noteId=3~10도 동일하게 작성)*

---

## 4단계 — Precision@4 / Recall@4 / F1 계산

각 임계값별로, 각 노트에 대해 계산한 뒤 평균을 낸다.

```
Precision@4 = (상위 4개 결과 중 Ground Truth ✅ 수) / 4
Recall@4    = (상위 4개 결과 중 Ground Truth ✅ 수) / (전체 Ground Truth ✅ 수)
F1          = 2 × (Precision × Recall) / (Precision + Recall)
```

### 집계표

| 임계값 | 평균 Precision@4 | 평균 Recall@4 | 평균 F1 |
|-------|----------------|--------------|---------|
| 0.60 | | | |
| 0.70 | | | |
| **0.75 (현재)** | | | |
| 0.80 | | | |
| 0.85 | | | |

---

## 5단계 — 판단 기준

**채택 기준**: 현재 0.75보다 F1이 **0.05 이상** 높은 값이 있으면 변경 권장

| 결과 | 조치 |
|------|------|
| 어떤 임계값도 F1 차이 < 0.05 | 0.75 현행 유지 |
| 낮은 임계값(0.60~0.70)의 F1이 높음 | 임계값 하향 → 더 많은 관련 노트 검색 |
| 높은 임계값(0.80~0.85)의 F1이 높음 | 임계값 상향 → 더 정확한 노트만 검색 |

---

## 6단계 — 측정 로그 제거

측정 완료 후 `RagRetrievalService.java`에서 아래 두 부분 제거:

1. `retrieve()` 내 `logThresholdCandidates(queryText, userId, excludeNoteId);` 호출
2. `logThresholdCandidates()` 메서드 전체
