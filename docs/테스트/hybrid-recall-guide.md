# Dense-only vs 하이브리드 Recall@4 비교 (#62)

## 측정 목적

하이브리드 검색(Dense + Sparse + RRF)이 Dense-only 대비 실제로 Recall@4를 얼마나 향상시키는지 수치로 확인한다.

- **Dense-only 모드**: `bm25-max-results: 0` 설정 → Sparse 비활성화
- **하이브리드 모드**: `bm25-max-results: 10` 설정 (기본값)

---

## 노트 카테고리 구성

### 카테고리 A — 코드 식별자 핵심 (noteId 11~15)
정확한 함수명·클래스명이 핵심인 노트. Dense 검색이 놓치고 Sparse가 잡을 것으로 예상.

```bash
# noteId=11: HashMap 구현
curl -s -X POST http://localhost:8080/api/notes \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "title": "HashMap put get 구현",
    "content": "HashMap의 put() 메서드는 키를 해시 함수에 통과시켜 버킷 인덱스를 계산한다. get()은 동일한 해시로 버킷을 찾아 equals()로 키를 비교한다. ```java\npublic V put(K key, V value) {\n    int hash = hash(key);\n    int index = hash & (capacity - 1);\n    Node<K,V> node = table[index];\n    // 충돌 처리: 체이닝\n    while (node != null) {\n        if (node.hash == hash && key.equals(node.key)) {\n            node.value = value;\n            return;\n        }\n        node = node.next;\n    }\n    table[index] = new Node<>(hash, key, value, table[index]);\n}\n```"
  }'

# noteId=12: factorial 재귀 구현
curl -s -X POST http://localhost:8080/api/notes \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "title": "factorial 재귀 구현",
    "content": "factorial 함수는 재귀로 구현할 수 있다. n이 0이면 1을 반환하는 기저 조건이 반드시 있어야 한다. ```java\npublic static int factorial(int n) {\n    if (n == 0) return 1;\n    return n * factorial(n - 1);\n}\n// factorial(5) = 120\n```\n메모이제이션을 적용하면 중복 계산을 피할 수 있다."
  }'

# noteId=13: BinarySearch 구현
curl -s -X POST http://localhost:8080/api/notes \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "title": "BinarySearch 반복문 구현",
    "content": "BinarySearch는 정렬된 배열에서 O(log n)으로 탐색한다. ```java\npublic static int binarySearch(int[] arr, int target) {\n    int left = 0, right = arr.length - 1;\n    while (left <= right) {\n        int mid = left + (right - left) / 2;\n        if (arr[mid] == target) return mid;\n        if (arr[mid] < target) left = mid + 1;\n        else right = mid - 1;\n    }\n    return -1;\n}\n```\nleft + (right - left) / 2 로 계산해야 정수 오버플로우를 방지할 수 있다."
  }'

# noteId=14: LinkedList Node 구현
curl -s -X POST http://localhost:8080/api/notes \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "title": "LinkedList Node 삽입 삭제",
    "content": "LinkedList의 Node는 data와 next 포인터로 구성된다. ```java\nclass Node<T> {\n    T data;\n    Node<T> next;\n    Node(T data) { this.data = data; }\n}\npublic void addFirst(T data) {\n    Node<T> newNode = new Node<>(data);\n    newNode.next = head;\n    head = newNode;\n    size++;\n}\npublic T removeFirst() {\n    T data = head.data;\n    head = head.next;\n    size--;\n    return data;\n}\n```"
  }'

# noteId=15: QuickSort 피벗 구현
curl -s -X POST http://localhost:8080/api/notes \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "title": "QuickSort 피벗 파티션 구현",
    "content": "QuickSort는 피벗을 기준으로 배열을 분할한 뒤 재귀적으로 정렬한다. ```java\npublic static void quickSort(int[] arr, int low, int high) {\n    if (low < high) {\n        int pivot = partition(arr, low, high);\n        quickSort(arr, low, pivot - 1);\n        quickSort(arr, pivot + 1, high);\n    }\n}\nprivate static int partition(int[] arr, int low, int high) {\n    int pivot = arr[high];\n    int i = low - 1;\n    for (int j = low; j < high; j++) {\n        if (arr[j] <= pivot) swap(arr, ++i, j);\n    }\n    swap(arr, i + 1, high);\n    return i + 1;\n}\n```"
  }'
```

### 카테고리 B — 짧은 한국어 CS 용어 핵심 (noteId 16~20)
"큐", "스택", "힙" 등 1~2자 한국어 용어가 핵심인 노트. Dense 검색이 짧은 단어를 놓칠 수 있음.

```bash
# noteId=16: 큐 활용 사례
curl -s -X POST http://localhost:8080/api/notes \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "title": "큐 활용 사례와 구현 방식",
    "content": "큐는 FIFO 방식으로 작동하며 BFS 구현, 프린터 스풀링, 캐시 교체 정책에 활용된다. 큐의 enqueue는 뒤에 추가, dequeue는 앞에서 제거한다. 원형 큐는 배열 낭비를 방지하며 (rear + 1) % capacity 공식으로 인덱스를 순환시킨다. 우선순위 큐는 힙으로 구현하며 O(log n)으로 삽입과 삭제를 처리한다."
  }'

# noteId=17: 스택 활용과 구현
curl -s -X POST http://localhost:8080/api/notes \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "title": "스택 활용과 구현 방식",
    "content": "스택은 LIFO 방식으로 작동하며 함수 호출 스택, 괄호 검사, 수식 평가에 활용된다. 스택의 push는 top에 추가, pop은 top에서 제거하며 모두 O(1)이다. 재귀를 명시적 스택으로 변환하면 스택 오버플로우를 방지할 수 있다. 단조 스택은 다음 큰 원소 찾기 등 최적화 문제에 쓰인다."
  }'

# noteId=18: 힙 연산과 활용
curl -s -X POST http://localhost:8080/api/notes \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "title": "힙 연산과 우선순위 큐",
    "content": "힙은 완전 이진 트리로 최대힙과 최소힙이 있다. 힙의 삽입은 마지막 위치에 추가 후 heapify-up으로 O(log n), 삭제는 루트 제거 후 heapify-down으로 O(log n)이다. 힙은 우선순위 큐 구현에 사용되며 다익스트라 알고리즘에서 최단 거리 노드를 빠르게 추출한다. k번째 최솟값 찾기는 크기 k의 최대힙으로 O(n log k)에 해결 가능하다."
  }'

# noteId=19: 트리 종류와 특성
curl -s -X POST http://localhost:8080/api/notes \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "title": "트리 종류와 특성 비교",
    "content": "트리는 계층적 자료구조로 루트, 내부 노드, 리프로 구성된다. 이진 탐색 트리(BST)는 왼쪽 < 부모 < 오른쪽 조건을 유지한다. 균형 트리인 AVL 트리와 Red-Black 트리는 O(log n)의 탐색을 보장한다. B-트리는 디스크 기반 데이터베이스 인덱스에 사용되며 노드당 여러 키를 저장한다. 힙도 트리의 일종이지만 BST 조건을 따르지 않는다."
  }'

# noteId=20: 그래프와 탐색
curl -s -X POST http://localhost:8080/api/notes \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "title": "그래프 표현과 탐색 비교",
    "content": "그래프는 정점(Vertex)과 간선(Edge)으로 구성된다. 인접 행렬은 O(1) 탐색, O(V²) 공간이며 인접 리스트는 O(V+E) 공간이다. BFS는 큐를 사용해 최단 경로를 탐색하고 DFS는 스택(재귀)으로 경로 탐색에 활용된다. 위상 정렬은 DAG에서 DFS 기반으로 구현하며 강한 연결 요소는 코사라주 알고리즘으로 찾는다."
  }'
```

---

## Ground Truth 정의 (수동 작업)

아래 표에서 각 쿼리 노트와 **실제로 연관된** 과거 노트를 ✅로 표시한다.
연관 기준: "이 노트를 공부한 학생에게 과거 노트를 참조한 연결 질문을 던질 수 있는가?"

### 카테고리 A Ground Truth

| 쿼리 노트 | 정답 연관 노트 | 정답 수 |
|---------|------------|-------|
| HashMap put/get 구현 | (직접 채워주세요) | |
| factorial 재귀 구현 | (직접 채워주세요) | |
| BinarySearch 구현 | (직접 채워주세요) | |
| LinkedList Node 구현 | (직접 채워주세요) | |
| QuickSort 피벗 구현 | (직접 채워주세요) | |

### 카테고리 B Ground Truth

| 쿼리 노트 | 정답 연관 노트 | 정답 수 |
|---------|------------|-------|
| 큐 활용 사례 | (직접 채워주세요) | |
| 스택 활용 | (직접 채워주세요) | |
| 힙 연산 | (직접 채워주세요) | |
| 트리 종류 | (직접 채워주세요) | |
| 그래프 탐색 | (직접 채워주세요) | |

---

## 측정 절차

### 1단계 — Dense-only 측정

`application.yml`에서 `bm25-max-results: 0` 설정 후 서버 재시작.

```bash
# 카테고리 A 노트 (noteId=11~15) 대화 시작
for i in {11..15}; do
  curl -s -X POST http://localhost:8080/api/v1/conversations \
    -H "Content-Type: application/json" \
    -d "{\"noteId\": $i, \"type\": \"QUESTION\"}"
  sleep 0.5
done

# 카테고리 B 노트 (noteId=16~20) 대화 시작
for i in {16..20}; do
  curl -s -X POST http://localhost:8080/api/v1/conversations \
    -H "Content-Type: application/json" \
    -d "{\"noteId\": $i, \"type\": \"QUESTION\"}"
  sleep 0.5
done
```

로그에서 `[#62] [Dense-only] 최종 Top-4:` 라인을 수집해 아래 표를 채운다.

### 2단계 — 하이브리드 측정

`application.yml`에서 `bm25-max-results: 10` 복원 후 서버 재시작.
동일한 curl 명령어 재실행. `[#62] [하이브리드] 최종 Top-4:` 라인 수집.

---

## 결과 기록표

### Dense-only 결과

| noteId | 쿼리 노트 | 카테고리 | Top-4 결과 | 정답 포함 수 | Recall@4 |
|--------|---------|---------|-----------|-----------|---------|
| 11 | HashMap put/get | A | | | |
| 12 | factorial 재귀 | A | | | |
| 13 | BinarySearch | A | | | |
| 14 | LinkedList Node | A | | | |
| 15 | QuickSort 피벗 | A | | | |
| 16 | 큐 활용 | B | | | |
| 17 | 스택 활용 | B | | | |
| 18 | 힙 연산 | B | | | |
| 19 | 트리 종류 | B | | | |
| 20 | 그래프 탐색 | B | | | |
| | **카테고리 A 평균** | | | | |
| | **카테고리 B 평균** | | | | |

### 하이브리드 결과

| noteId | 쿼리 노트 | 카테고리 | Top-4 결과 | 정답 포함 수 | Recall@4 |
|--------|---------|---------|-----------|-----------|---------|
| 11 | HashMap put/get | A | | | |
| 12 | factorial 재귀 | A | | | |
| 13 | BinarySearch | A | | | |
| 14 | LinkedList Node | A | | | |
| 15 | QuickSort 피벗 | A | | | |
| 16 | 큐 활용 | B | | | |
| 17 | 스택 활용 | B | | | |
| 18 | 힙 연산 | B | | | |
| 19 | 트리 종류 | B | | | |
| 20 | 그래프 탐색 | B | | | |
| | **카테고리 A 평균** | | | | |
| | **카테고리 B 평균** | | | | |

---

## Recall@4 계산 방법

```
Recall@4 = (Top-4 결과에 포함된 정답 노트 수) / (전체 정답 노트 수)
```

예시: 정답 노트가 3개인데 Top-4에 2개 포함 → Recall@4 = 2/3 = 0.67

---

## 판단 기준

| 조건 | 판단 |
|------|------|
| 카테고리 A 또는 B에서 하이브리드 Recall@4 - Dense Recall@4 ≥ 0.1 | 하이브리드 효과 확인 ✅ |
| 두 카테고리 모두 차이 < 0.1 | 하이브리드 추가 비용 재검토 |
| 하이브리드가 Dense보다 낮은 Recall | 2차 필터 설정 재검토 필요 |

---

## 측정 로그 제거 (완료 후)

`RagRetrievalService.java`에서 `[#62]` 주석이 달린 로그 블록 2개 제거:
1. `retrieve()` 내 Dense/Sparse 건수 로그
2. `retrieve()` 내 최종 Top-4 로그
3. `sparseSearch()` 내 `if (bm25MaxResults == 0) return List.of();` 라인
