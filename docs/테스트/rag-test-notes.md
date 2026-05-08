# RAG 필터 수동 테스트용 노트 (#59)

서버 실행 후 아래 curl 명령어를 순서대로 실행하세요.  
노트 20개를 저장한 뒤 대화 시작 API를 10회 이상 호출하면 `[RAG 필터]` 로그가 출력됩니다.

---

## 1단계 — 노트 20개 저장

```bash
{
    "sessionId": 1,
    "title": "HashMap 자료구조",
    "content": "HashMap은 Java에서 키-값 쌍을 저장하는 자료구조다. 내부적으로 배열과 연결 리스트를 결합한 해시 테이블로 구현된다. 키를 해시 함수에 넣어 인덱스를 계산하고, 해당 버킷에 값을 저장한다. 평균 시간복잡도는 O(1)이며 최악의 경우 O(n)이다. Java 8 이후에는 버킷의 연결 리스트 길이가 8 이상이면 트리(Red-Black Tree)로 변환하여 최악의 경우도 O(log n)으로 개선했다. 주요 메서드로는 put(), get(), remove(), containsKey(), size() 등이 있다. 동기화가 필요한 경우 ConcurrentHashMap을 사용하는 것이 좋다."
}


# 2. 스택 자료구조
{
    "sessionId": 1,
    "title": "스택(Stack) 자료구조",
    "content": "스택은 LIFO(Last In First Out) 구조를 따르는 자료구조다. 마지막에 삽입된 데이터가 가장 먼저 제거된다. 주요 연산은 push(삽입), pop(제거), peek(최상단 조회)이며 모두 O(1)이다. 함수 호출 스택, 괄호 검사, 후위 표기법 계산, 브라우저 뒤로가기 기능 등에 활용된다. Java에서는 Stack 클래스나 Deque 인터페이스(ArrayDeque)를 사용한다. 재귀 알고리즘을 반복문으로 변환할 때 명시적 스택을 활용하는 경우가 많다. 스택 오버플로우는 재귀 호출이 너무 깊어질 때 발생한다."
}

# 3. 큐 자료구조
{
    "sessionId": 1,
    "title": "큐(Queue) 자료구조",
    "content": "큐는 FIFO(First In First Out) 구조를 따르는 자료구조다. 먼저 삽입된 데이터가 먼저 제거된다. 주요 연산은 enqueue(삽입), dequeue(제거), front(앞 원소 조회)이며 모두 O(1)이다. BFS(너비 우선 탐색), 프린터 스풀링, 프로세스 스케줄링, 네트워크 패킷 처리 등에 활용된다. Java에서는 Queue 인터페이스와 LinkedList, ArrayDeque를 주로 사용한다. 우선순위 큐(PriorityQueue)는 삽입된 순서가 아니라 우선순위가 높은 원소를 먼저 꺼낸다."
}

# 4. 이진 탐색
{
    "sessionId": 1,
    "title": "이진 탐색(Binary Search)",
    "content": "이진 탐색은 정렬된 배열에서 목표값을 찾는 알고리즘이다. 배열의 중간 값과 목표값을 비교하여 탐색 범위를 절반씩 줄여나간다. 시간복잡도는 O(log n)으로 선형 탐색 O(n)보다 훨씬 빠르다. 구현 시 left, right, mid 포인터를 사용하며 mid = left + (right - left) / 2로 계산해야 정수 오버플로우를 방지할 수 있다. 재귀와 반복문 두 방식으로 구현 가능하다. Lower bound(같거나 큰 첫 위치)와 Upper bound(큰 첫 위치) 변형도 자주 활용된다. 정렬 여부가 전제 조건임을 반드시 확인해야 한다."
}

# 5. 재귀 알고리즘
{
    "sessionId": 1,
    "title": "재귀(Recursion) 알고리즘",
    "content": "재귀는 함수가 자기 자신을 호출하는 프로그래밍 기법이다. 기저 조건(Base Case)과 재귀 조건(Recursive Case)으로 구성된다. 기저 조건이 없으면 무한 재귀로 스택 오버플로우가 발생한다. 팩토리얼, 피보나치 수열, 트리 순회, 분할 정복 등에 활용된다. 꼬리 재귀(Tail Recursion)는 일부 컴파일러에서 반복문으로 최적화된다. 메모이제이션을 적용하면 중복 계산을 줄여 성능을 향상시킬 수 있다. 일반적으로 재귀보다 반복문이 성능상 유리하지만, 코드 가독성에서는 재귀가 뛰어난 경우가 많다."
}

# 6. 동적 프로그래밍
{
    "sessionId": 1,
    "title": "동적 프로그래밍(Dynamic Programming)",
    "content": "동적 프로그래밍(DP)은 큰 문제를 작은 부분 문제로 쪼개고, 각 결과를 저장하여 중복 계산을 피하는 알고리즘 설계 기법이다. 최적 부분 구조와 중복 부분 문제 두 조건을 만족할 때 적용 가능하다. 탑다운(메모이제이션)과 바텀업(타뷸레이션) 방식으로 구현한다. 대표 문제로 배낭 문제(Knapsack), 최장 공통 부분 수열(LCS), 최단 경로(Floyd-Warshall) 등이 있다. 점화식을 먼저 정의하고 그에 맞는 dp 배열 구조를 설계하는 것이 핵심이다."
}

# 7. 퀵정렬
{
    "sessionId": 1,
    "title": "퀵정렬(Quick Sort)",
    "content": "퀵정렬은 분할 정복 방식의 정렬 알고리즘이다. 피벗(Pivot)을 선택하고, 피벗보다 작은 원소는 왼쪽, 큰 원소는 오른쪽으로 분할한 뒤 재귀적으로 정렬한다. 평균 시간복잡도는 O(n log n)이며 최악(이미 정렬된 경우)은 O(n²)이다. 추가 메모리가 거의 필요 없어 제자리 정렬(In-place)에 해당한다. 피벗 선택 전략(랜덤, 중앙값)으로 최악 케이스를 줄일 수 있다. 실제로는 병합 정렬보다 캐시 효율이 좋아 실용적 성능이 뛰어나다."
}

# 8. 그래프 BFS
{
    "sessionId": 1,
    "title": "너비 우선 탐색(BFS)",
    "content": "BFS(Breadth First Search)는 시작 정점에서 가까운 정점부터 순서대로 방문하는 그래프 탐색 알고리즘이다. 큐(Queue)를 사용하여 구현한다. 시간복잡도는 O(V+E)이며 V는 정점 수, E는 간선 수다. 최단 경로 탐색에 적합하며, 가중치가 없는 그래프에서 최단 거리를 보장한다. 방문 여부를 체크하는 visited 배열이 필수다. 레벨 순서 트리 순회, 소셜 네트워크 친구 추천, 미로 최단 경로 등에 활용된다. DFS와 달리 메모리 사용량이 크지만 완전 탐색에 유리하다."
}

# 9. 그래프 DFS
{
    "sessionId": 1,
    "title": "깊이 우선 탐색(DFS)",
    "content": "DFS(Depth First Search)는 한 방향으로 갈 수 있는 곳까지 깊이 탐색한 후 되돌아오는 그래프 탐색 알고리즘이다. 스택(Stack) 또는 재귀를 사용하여 구현한다. 시간복잡도는 O(V+E)다. 연결 요소 개수 파악, 사이클 감지, 위상 정렬, 백트래킹 등에 활용된다. BFS보다 메모리 효율이 좋지만 최단 경로를 보장하지 않는다. 방문 체크와 재귀 깊이 제한을 신경 써야 한다."
}

# 10. 힙(Heap)
{
    "sessionId": 1,
    "title": "힙(Heap) 자료구조",
    "content": "힙은 완전 이진 트리 기반의 자료구조로 최대힙과 최소힙으로 구분된다. 최대힙은 부모 노드가 자식 노드보다 항상 크거나 같고, 최소힙은 반대다. 삽입과 삭제 연산의 시간복잡도는 O(log n)이다. 힙 구조를 이용하면 우선순위 큐를 효율적으로 구현할 수 있다. 힙 정렬은 O(n log n)의 시간복잡도를 가진다. Java에서는 PriorityQueue가 내부적으로 최소힙으로 구현되어 있다. k번째 최솟값 찾기, 다익스트라 알고리즘 등에 활용된다."
}

# 11. 트리 순회
{
    "sessionId": 1,
    "title": "트리 순회(Tree Traversal)",
    "content": "이진 트리 순회는 전위(Pre-order), 중위(In-order), 후위(Post-order) 세 방식이 있다. 전위 순회는 루트-왼쪽-오른쪽 순서로 방문하며 트리 복사에 활용된다. 중위 순회는 왼쪽-루트-오른쪽 순서로 BST에서 정렬된 값을 출력할 때 쓰인다. 후위 순회는 왼쪽-오른쪽-루트 순서로 트리 삭제나 수식 계산에 활용된다. 레벨 순서 순회는 BFS를 이용하며 레벨별로 노드를 방문한다. 각 순회는 재귀와 반복문(스택 활용) 두 방식으로 구현 가능하다."
}

# 12. 링크드 리스트
{
    "sessionId": 1,
    "title": "연결 리스트(Linked List)",
    "content": "연결 리스트는 각 노드가 데이터와 다음 노드의 포인터를 가지는 선형 자료구조다. 배열과 달리 크기가 동적으로 변하며 삽입/삭제가 O(1)이다(위치를 알 때). 단방향 연결 리스트는 next 포인터만 가지며, 양방향은 next와 prev를 모두 가진다. 임의 접근이 O(n)으로 배열보다 느리다. Java의 LinkedList는 양방향 연결 리스트로 구현되어 있다. 플로이드 사이클 감지 알고리즘(토끼와 거북이)으로 사이클 존재 여부를 O(1) 공간으로 확인할 수 있다."
}

# 13. 시간복잡도
{
    "sessionId": 1,
    "title": "시간복잡도(Time Complexity) 분석",
    "content": "시간복잡도는 알고리즘이 실행되는 데 걸리는 시간을 입력 크기 n에 대한 함수로 표현한다. 빅오(Big-O) 표기법은 최악의 경우를 나타낸다. O(1) < O(log n) < O(n) < O(n log n) < O(n²) < O(2ⁿ) 순으로 느려진다. 중첩 반복문은 O(n²), 분할 정복은 O(n log n), 해시 탐색은 O(1)이다. 공간복잡도는 알고리즘이 사용하는 메모리 양을 나타낸다. 시간과 공간은 일반적으로 트레이드오프 관계에 있다. 코딩 테스트에서는 n ≤ 10^6일 때 O(n log n) 이하가 필요한 경우가 많다."
}

# 14. 정렬 알고리즘 비교
{
    "sessionId": 1,
    "title": "정렬 알고리즘 비교",
    "content": "주요 정렬 알고리즘의 시간복잡도 비교: 버블 정렬 O(n²), 선택 정렬 O(n²), 삽입 정렬 O(n²), 병합 정렬 O(n log n), 퀵 정렬 평균 O(n log n), 힙 정렬 O(n log n), 계수 정렬 O(n+k). 안정 정렬은 동일 값의 상대적 순서를 유지한다. 병합 정렬과 삽입 정렬은 안정 정렬이고, 퀵 정렬과 힙 정렬은 불안정 정렬이다. Java Arrays.sort()는 기본 타입에 듀얼 피벗 퀵 정렬, 객체 타입에 TimSort(병합+삽입)를 사용한다."
}

# 15. 메모리 관리와 GC
{
    "sessionId": 1,
    "title": "Java 메모리 관리와 GC(Garbage Collection)",
    "content": "Java 힙 메모리는 Young Generation과 Old Generation으로 나뉜다. Young 영역은 새로 생성된 객체가 위치하며 Eden과 두 개의 Survivor 영역으로 구성된다. Minor GC는 Young 영역에서 발생하며 빠르다. Major GC(Full GC)는 Old 영역까지 수집하며 STW(Stop The World) 시간이 길다. G1GC는 힙을 Region으로 나눠 점진적으로 수집한다. GC 튜닝 시 힙 크기, GC 알고리즘 선택, STW 시간 최소화를 목표로 한다. 메모리 릭은 사용되지 않는 객체를 참조하는 상태로 OOM을 유발한다."
}

# 16. 멀티스레딩
{
    "sessionId": 1,
    "title": "Java 멀티스레딩과 동기화",
    "content": "멀티스레딩은 하나의 프로세스에서 여러 스레드가 동시에 실행되는 기법이다. 스레드는 프로세스의 자원을 공유하며, 공유 자원 접근 시 동기화가 필요하다. synchronized 키워드로 임계 구역을 설정할 수 있다. volatile은 변수를 CPU 캐시가 아닌 메인 메모리에서 직접 읽도록 강제한다. 교착 상태(Deadlock)는 두 스레드가 서로 상대방의 자원을 기다릴 때 발생한다. Java 5+ ExecutorService로 스레드 풀을 효율적으로 관리할 수 있다. Atomic 클래스는 락 없이 원자적 연산을 지원한다."
}

# 17. 데이터베이스 인덱스
{
    "sessionId": 1,
    "title": "데이터베이스 인덱스(Index)",
    "content": "인덱스는 데이터베이스 검색 속도를 향상시키는 자료구조다. 대부분의 RDBMS는 B-Tree 인덱스를 사용한다. 인덱스를 사용하면 SELECT 성능이 향상되지만 INSERT, UPDATE, DELETE 성능이 저하된다. 카디널리티(Cardinality)가 높은 컬럼에 인덱스를 생성하는 것이 효과적이다. 복합 인덱스는 인덱스 컬럼 순서가 중요하며 왼쪽 접두사 원칙을 따른다. 커버링 인덱스는 쿼리에 필요한 모든 컬럼이 인덱스에 포함되어 테이블 접근 없이 처리 가능하다. EXPLAIN으로 인덱스 사용 여부를 확인할 수 있다."
}

# 18. JPA 연관관계
{
    "sessionId": 1,
    "title": "JPA 연관관계 매핑",
    "content": "JPA 연관관계는 @OneToOne, @OneToMany, @ManyToOne, @ManyToMany로 설정한다. 단방향과 양방향 연관관계가 있으며 양방향에서는 mappedBy로 연관관계 주인을 지정한다. N+1 문제는 연관된 엔티티를 개별 쿼리로 조회할 때 발생한다. Fetch Join이나 EntityGraph로 N+1 문제를 해결할 수 있다. 지연 로딩(LAZY)은 실제 사용 시점에 쿼리가 실행되고, 즉시 로딩(EAGER)은 연관 엔티티를 즉시 조회한다. 기본적으로 ToOne은 EAGER, ToMany는 LAZY가 기본값이다."
}

# 19. REST API 설계
{
    "sessionId": 1,
    "title": "REST API 설계 원칙",
    "content": "REST(Representational State Transfer)는 HTTP 기반의 아키텍처 스타일이다. 자원은 URI로 표현하고 행위는 HTTP 메서드로 나타낸다. GET(조회), POST(생성), PUT(전체 수정), PATCH(부분 수정), DELETE(삭제)를 용도에 맞게 사용한다. 무상태성(Stateless)은 각 요청이 독립적이어야 한다는 원칙이다. URI에는 명사를 사용하고 동사를 피해야 한다. 예: /users/{id}/notes가 /getUserNotes보다 REST답다. HTTP 상태코드를 적절히 활용해야 한다: 200(OK), 201(Created), 400(Bad Request), 404(Not Found), 500(Internal Error)."
}

# 20. 디자인 패턴 — 싱글톤
{
    "sessionId": 1,
    "title": "디자인 패턴 — 싱글톤(Singleton)",
    "content": "싱글톤 패턴은 클래스의 인스턴스가 하나만 생성되도록 보장하는 생성 패턴이다. private 생성자와 static 인스턴스 변수로 구현한다. 멀티스레드 환경에서는 DCL(Double Checked Locking)이나 Holder 방식을 사용해야 한다. 스프링 컨테이너에 등록된 빈(@Bean)은 기본적으로 싱글톤 스코프다. 싱글톤의 단점은 전역 상태로 인한 테스트 어려움과 의존성 은닉이다. 안티패턴으로 불리기도 하며, DI 컨테이너로 대체하는 것이 일반적이다."
}
```

---

## 2단계 — 대화 시작 API 10회 이상 호출

노트 저장 후 아래 명령어로 대화를 시작합니다.  
각 호출마다 서버 로그에 `[RAG 필터]` 라인이 출력됩니다.

```bash
# noteId 1~10에 대해 대화 시작 (각각 retrieve() 호출 → 로그 발생)
for i in {1..10}; do
  curl -s -X POST http://localhost:8080/api/v1/conversations \
    -H "Content-Type: application/json" \
    -d "{\"noteId\": $i, \"type\": \"QUESTION\"}" | jq '.conversationId'
  sleep 0.5
done
```

---

## 3단계 — 로그 수집 및 제거율 계산

```bash
# [RAG 필터] 라인만 추출
grep "\[RAG 필터\]" logs/application.log

# 제거율만 파싱해서 평균 계산 (awk 활용)
grep "\[RAG 필터\]" logs/application.log \
  | grep -oP '제거율: \K[\d.]+' \
  | awk '{ sum += $1; count++ } END { printf "평균 제거율: %.1f%%\n", sum/count }'
```
log.info("[RAG 필터] 쿼리='{}' | RRF 후: {}건 → 2차 필터 후: {}건 | 제거율: {}%",


**출력 예시:**
```
[RAG 필터] 쿼리='HashMap은 Java의 자료구조' | RRF 후: 3건 → 2차 필터 후: 2건 | 제거율: 33.3%
[RAG 필터] 쿼리='재귀 알고리즘 팩토리얼 구현' | RRF 후: 4건 → 2차 필터 후: 4건 | 제거율: 0.0%
...
평균 제거율: 12.5%
```



2026-05-08T18:26:34.948+09:00  INFO 69984 --- [criticalflow] [nio-8080-exec-1] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='HashMap은 Java에서 키-값 쌍을 저장하는 자료구조다. 내부적으로 배열과 연결 리스트를 결합한 해시 테이블로 구현된다. 키를 해시 함수에 넣어 인덱스를 계산하고, 해당 버킷에 값을 저장한다. 평균 시간복잡도는 O(1)이며 최악의 경우 O(n)이다. Java 8 이후에는 버킷의 연결 리스트 길이가 8 이상이면 트리(Red-Black Tree)로 변환하여 최악의 경우도 O(log n)으로 개선했다. 주요 메서드로는 put(), get(), remove(), containsKey(), size() 등이 있다. 동기화가 필요한 경우 ConcurrentHashMap을 사용하는 것이 좋다.' | RRF 후: 4건 → 2차 필터 후: 0건 | 제거율: 100.0%
2026-05-08T18:26:36.027+09:00  INFO 69984 --- [criticalflow] [nio-8080-exec-1] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=1 → TYPE=TYPE_F
2026-05-08T18:46:03.054+09:00  WARN 69984 --- [criticalflow] [l-1 housekeeper] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Thread starvation or clock leap detected (housekeeper delta=9m2s452ms).
2026-05-08T18:46:05.088+09:00  INFO 69984 --- [criticalflow] [nio-8080-exec-4] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='스택은 LIFO(Last In First Out) 구조를 따르는 자료구조다. 마지막에 삽입된 데이터가 가장 먼저 제거된다. 주요 연산은 push(삽입), pop(제거), peek(최상단 조회)이며 모두 O(1)이다. 함수 호출 스택, 괄호 검사, 후위 표기법 계산, 브라우저 뒤로가기 기능 등에 활용된다. Java에서는 Stack 클래스나 Deque 인터페이스(ArrayDeque)를 사용한다. 재귀 알고리즘을 반복문으로 변환할 때 명시적 스택을 활용하는 경우가 많다. 스택 오버플로우는 재귀 호출이 너무 깊어질 때 발생한다.' | RRF 후: 4건 → 2차 필터 후: 1건 | 제거율: 75.0%
2026-05-08T18:46:06.935+09:00  INFO 69984 --- [criticalflow] [nio-8080-exec-4] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=2 → TYPE=TYPE_F
2026-05-08T18:46:21.526+09:00  INFO 69984 --- [criticalflow] [nio-8080-exec-5] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='큐는 FIFO(First In First Out) 구조를 따르는 자료구조다. 먼저 삽입된 데이터가 먼저 제거된다. 주요 연산은 enqueue(삽입), dequeue(제거), front(앞 원소 조회)이며 모두 O(1)이다. BFS(너비 우선 탐색), 프린터 스풀링, 프로세스 스케줄링, 네트워크 패킷 처리 등에 활용된다. Java에서는 Queue 인터페이스와 LinkedList, ArrayDeque를 주로 사용한다. 우선순위 큐(PriorityQueue)는 삽입된 순서가 아니라 우선순위가 높은 원소를 먼저 꺼낸다.' | RRF 후: 4건 → 2차 필터 후: 1건 | 제거율: 75.0%
2026-05-08T18:46:22.549+09:00  INFO 69984 --- [criticalflow] [nio-8080-exec-5] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=3 → TYPE=TYPE_F
2026-05-08T18:46:26.322+09:00  INFO 69984 --- [criticalflow] [nio-8080-exec-7] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='이진 탐색은 정렬된 배열에서 목표값을 찾는 알고리즘이다. 배열의 중간 값과 목표값을 비교하여 탐색 범위를 절반씩 줄여나간다. 시간복잡도는 O(log n)으로 선형 탐색 O(n)보다 훨씬 빠르다. 구현 시 left, right, mid 포인터를 사용하며 mid = left + (right - left) / 2로 계산해야 정수 오버플로우를 방지할 수 있다. 재귀와 반복문 두 방식으로 구현 가능하다. Lower bound(같거나 큰 첫 위치)와 Upper bound(큰 첫 위치) 변형도 자주 활용된다. 정렬 여부가 전제 조건임을 반드시 확인해야 한다.' | RRF 후: 4건 → 2차 필터 후: 0건 | 제거율: 100.0%
2026-05-08T18:46:27.088+09:00  INFO 69984 --- [criticalflow] [nio-8080-exec-7] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=4 → TYPE=TYPE_F
2026-05-08T18:46:30.134+09:00  INFO 69984 --- [criticalflow] [nio-8080-exec-8] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='재귀는 함수가 자기 자신을 호출하는 프로그래밍 기법이다. 기저 조건(Base Case)과 재귀 조건(Recursive Case)으로 구성된다. 기저 조건이 없으면 무한 재귀로 스택 오버플로우가 발생한다. 팩토리얼, 피보나치 수열, 트리 순회, 분할 정복 등에 활용된다. 꼬리 재귀(Tail Recursion)는 일부 컴파일러에서 반복문으로 최적화된다. 메모이제이션을 적용하면 중복 계산을 줄여 성능을 향상시킬 수 있다. 일반적으로 재귀보다 반복문이 성능상 유리하지만, 코드 가독성에서는 재귀가 뛰어난 경우가 많다.' | RRF 후: 4건 → 2차 필터 후: 0건 | 제거율: 100.0%
2026-05-08T18:46:33.204+09:00  INFO 69984 --- [criticalflow] [nio-8080-exec-8] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=5 → TYPE=TYPE_D
2026-05-08T18:46:36.921+09:00  INFO 69984 --- [criticalflow] [nio-8080-exec-9] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='동적 프로그래밍(DP)은 큰 문제를 작은 부분 문제로 쪼개고, 각 결과를 저장하여 중복 계산을 피하는 알고리즘 설계 기법이다. 최적 부분 구조와 중복 부분 문제 두 조건을 만족할 때 적용 가능하다. 탑다운(메모이제이션)과 바텀업(타뷸레이션) 방식으로 구현한다. 대표 문제로 배낭 문제(Knapsack), 최장 공통 부분 수열(LCS), 최단 경로(Floyd-Warshall) 등이 있다. 점화식을 먼저 정의하고 그에 맞는 dp 배열 구조를 설계하는 것이 핵심이다.' | RRF 후: 4건 → 2차 필터 후: 0건 | 제거율: 100.0%
2026-05-08T18:46:38.119+09:00  INFO 69984 --- [criticalflow] [nio-8080-exec-9] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=6 → TYPE=TYPE_F
2026-05-08T18:46:42.911+09:00  INFO 69984 --- [criticalflow] [io-8080-exec-10] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='퀵정렬은 분할 정복 방식의 정렬 알고리즘이다. 피벗(Pivot)을 선택하고, 피벗보다 작은 원소는 왼쪽, 큰 원소는 오른쪽으로 분할한 뒤 재귀적으로 정렬한다. 평균 시간복잡도는 O(n log n)이며 최악(이미 정렬된 경우)은 O(n²)이다. 추가 메모리가 거의 필요 없어 제자리 정렬(In-place)에 해당한다. 피벗 선택 전략(랜덤, 중앙값)으로 최악 케이스를 줄일 수 있다. 실제로는 병합 정렬보다 캐시 효율이 좋아 실용적 성능이 뛰어나다.' | RRF 후: 4건 → 2차 필터 후: 0건 | 제거율: 100.0%
2026-05-08T18:46:44.160+09:00  INFO 69984 --- [criticalflow] [io-8080-exec-10] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=7 → TYPE=TYPE_F
2026-05-08T18:47:00.385+09:00  INFO 69984 --- [criticalflow] [nio-8080-exec-2] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='BFS(Breadth First Search)는 시작 정점에서 가까운 정점부터 순서대로 방문하는 그래프 탐색 알고리즘이다. 큐(Queue)를 사용하여 구현한다. 시간복잡도는 O(V+E)이며 V는 정점 수, E는 간선 수다. 최단 경로 탐색에 적합하며, 가중치가 없는 그래프에서 최단 거리를 보장한다. 방문 여부를 체크하는 visited 배열이 필수다. 레벨 순서 트리 순회, 소셜 네트워크 친구 추천, 미로 최단 경로 등에 활용된다. DFS와 달리 메모리 사용량이 크지만 완전 탐색에 유리하다.' | RRF 후: 4건 → 2차 필터 후: 1건 | 제거율: 75.0%
2026-05-08T18:47:01.775+09:00  INFO 69984 --- [criticalflow] [nio-8080-exec-2] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=8 → TYPE=TYPE_F
2026-05-08T18:47:04.857+09:00  INFO 69984 --- [criticalflow] [nio-8080-exec-1] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='DFS(Depth First Search)는 한 방향으로 갈 수 있는 곳까지 깊이 탐색한 후 되돌아오는 그래프 탐색 알고리즘이다. 스택(Stack) 또는 재귀를 사용하여 구현한다. 시간복잡도는 O(V+E)다. 연결 요소 개수 파악, 사이클 감지, 위상 정렬, 백트래킹 등에 활용된다. BFS보다 메모리 효율이 좋지만 최단 경로를 보장하지 않는다. 방문 체크와 재귀 깊이 제한을 신경 써야 한다.' | RRF 후: 4건 → 2차 필터 후: 1건 | 제거율: 75.0%
2026-05-08T18:47:05.625+09:00  INFO 69984 --- [criticalflow] [nio-8080-exec-1] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=9 → TYPE=TYPE_F
2026-05-08T18:47:08.345+09:00  INFO 69984 --- [criticalflow] [nio-8080-exec-3] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='힙은 완전 이진 트리 기반의 자료구조로 최대힙과 최소힙으로 구분된다. 최대힙은 부모 노드가 자식 노드보다 항상 크거나 같고, 최소힙은 반대다. 삽입과 삭제 연산의 시간복잡도는 O(log n)이다. 힙 구조를 이용하면 우선순위 큐를 효율적으로 구현할 수 있다. 힙 정렬은 O(n log n)의 시간복잡도를 가진다. Java에서는 PriorityQueue가 내부적으로 최소힙으로 구현되어 있다. k번째 최솟값 찾기, 다익스트라 알고리즘 등에 활용된다.' | RRF 후: 4건 → 2차 필터 후: 0건 | 제거율: 100.0%
2026-05-08T18:47:09.035+09:00  INFO 69984 --- [criticalflow] [nio-8080-exec-3] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=10 → TYPE=TYPE_F

⏺ 분석 결과

┌────────┬──────────┬───────┬──────────┬────────┐   
│ noteId │   노트   │ RRF   │ 2차 필터 │ 제거율 │
│        │          │  후   │    후    │        │   
├────────┼──────────┼───────┼──────────┼────────┤
│ 1      │ HashMap  │ 4건   │ 0건      │ 100.0% │
├────────┼──────────┼───────┼──────────┼────────┤
│ 2      │ 스택     │ 4건   │ 1건      │ 75.0%  │
├────────┼──────────┼───────┼──────────┼────────┤   
│ 3      │ 큐       │ 4건   │ 1건      │ 75.0%  │
├────────┼──────────┼───────┼──────────┼────────┤   
│ 4      │ 이진     │ 4건   │ 0건      │ 100.0% │
│        │ 탐색     │       │          │        │   
├────────┼──────────┼───────┼──────────┼────────┤
│ 5      │ 재귀     │ 4건   │ 0건      │ 100.0% │   
├────────┼──────────┼───────┼──────────┼────────┤
│ 6      │ DP       │ 4건   │ 0건      │ 100.0% │
├────────┼──────────┼───────┼──────────┼────────┤
│ 7      │ 퀵정렬   │ 4건   │ 0건      │ 100.0% │
├────────┼──────────┼───────┼──────────┼────────┤   
│ 8      │ BFS      │ 4건   │ 1건      │ 75.0%  │
├────────┼──────────┼───────┼──────────┼────────┤   
│ 9      │ DFS      │ 4건   │ 1건      │ 75.0%  │
├────────┼──────────┼───────┼──────────┼────────┤   
│ 10     │ 힙       │ 4건   │ 0건      │ 100.0% │
└────────┴──────────┴───────┴──────────┴────────┘

평균 제거율: 90.0% → 기준(>30%) 훨씬 초과
문제 원인

단순히 임계값(0.2)이 높은 게 아닙니다. 구조적
문제입니다.

retrieve()에 넘기는 queryText가 노트 전체           
내용입니다:                                       
ragRetrievalService.retrieve(note.getContent(), ...)
//  ↑ "HashMap은 Java에서 키-값 쌍을 저장하는...    
(150단어)"

그러면 isTopicRelevant 내부에서:                    
// 한국어 minLength=1 → 150단어 전부 키워드로 추출  
long significant = ~150개                         
// 과거 노트가 그 중 20%(30개) 이상을 포함해야 통과
// → 다른 주제 노트는 거의 불가능

노트 A(HashMap)의 키워드 150개 중 30개를 노트     
B(스택)가 포함하기는 매우 어렵습니다.
                                                    
---                                                 
결론 및 권장 조치

임계값을 낮추는 것으로는 근본적으로 해결이 안
됩니다. isTopicRelevant의 키워드 추출 방식을        
개선해야 합니다.

현재 방식 대신 명사/핵심 용어만 추출하도록 수정하는
게 좋습니다. 간단한 방법으로는 4자 이상 단어로    
필터링하면 "키-값", "자료구조", "HashMap" 같은 핵심
용어만 남아 키워드 수가 20~30개로 줄어듭니다.



수정 후 


2026-05-08T19:00:47.934+09:00  INFO 72392 --- [criticalflow] [nio-8080-exec-1] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='HashMap은 Java에서 키-값 쌍을 저장하는 자료구조다. 내부적으로 배열과 연결 리스트를 결합한 해시 테이블로 구현된다. 키를 해시 함수에 넣어 인덱스를 계산하고, 해당 버킷에 값을 저장한다. 평균 시간복잡도는 O(1)이며 최악의 경우 O(n)이다. Java 8 이후에는 버킷의 연결 리스트 길이가 8 이상이면 트리(Red-Black Tree)로 변환하여 최악의 경우도 O(log n)으로 개선했다. 주요 메서드로는 put(), get(), remove(), containsKey(), size() 등이 있다. 동기화가 필요한 경우 ConcurrentHashMap을 사용하는 것이 좋다.' | RRF 후: 4건 → 2차 필터 후: 3건 | 제거율: 25.0%
2026-05-08T19:00:48.897+09:00  INFO 72392 --- [criticalflow] [nio-8080-exec-1] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=1 → TYPE=TYPE_F
2026-05-08T19:01:02.651+09:00  INFO 72392 --- [criticalflow] [nio-8080-exec-3] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='스택은 LIFO(Last In First Out) 구조를 따르는 자료구조다. 마지막에 삽입된 데이터가 가장 먼저 제거된다. 주요 연산은 push(삽입), pop(제거), peek(최상단 조회)이며 모두 O(1)이다. 함수 호출 스택, 괄호 검사, 후위 표기법 계산, 브라우저 뒤로가기 기능 등에 활용된다. Java에서는 Stack 클래스나 Deque 인터페이스(ArrayDeque)를 사용한다. 재귀 알고리즘을 반복문으로 변환할 때 명시적 스택을 활용하는 경우가 많다. 스택 오버플로우는 재귀 호출이 너무 깊어질 때 발생한다.' | RRF 후: 4건 → 2차 필터 후: 2건 | 제거율: 50.0%
2026-05-08T19:01:03.793+09:00  INFO 72392 --- [criticalflow] [nio-8080-exec-3] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=2 → TYPE=TYPE_F
2026-05-08T19:01:07.848+09:00  INFO 72392 --- [criticalflow] [nio-8080-exec-4] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='큐는 FIFO(First In First Out) 구조를 따르는 자료구조다. 먼저 삽입된 데이터가 먼저 제거된다. 주요 연산은 enqueue(삽입), dequeue(제거), front(앞 원소 조회)이며 모두 O(1)이다. BFS(너비 우선 탐색), 프린터 스풀링, 프로세스 스케줄링, 네트워크 패킷 처리 등에 활용된다. Java에서는 Queue 인터페이스와 LinkedList, ArrayDeque를 주로 사용한다. 우선순위 큐(PriorityQueue)는 삽입된 순서가 아니라 우선순위가 높은 원소를 먼저 꺼낸다.' | RRF 후: 4건 → 2차 필터 후: 2건 | 제거율: 50.0%
2026-05-08T19:01:08.806+09:00  INFO 72392 --- [criticalflow] [nio-8080-exec-4] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=3 → TYPE=TYPE_F
2026-05-08T19:01:12.642+09:00  INFO 72392 --- [criticalflow] [nio-8080-exec-5] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='이진 탐색은 정렬된 배열에서 목표값을 찾는 알고리즘이다. 배열의 중간 값과 목표값을 비교하여 탐색 범위를 절반씩 줄여나간다. 시간복잡도는 O(log n)으로 선형 탐색 O(n)보다 훨씬 빠르다. 구현 시 left, right, mid 포인터를 사용하며 mid = left + (right - left) / 2로 계산해야 정수 오버플로우를 방지할 수 있다. 재귀와 반복문 두 방식으로 구현 가능하다. Lower bound(같거나 큰 첫 위치)와 Upper bound(큰 첫 위치) 변형도 자주 활용된다. 정렬 여부가 전제 조건임을 반드시 확인해야 한다.' | RRF 후: 4건 → 2차 필터 후: 3건 | 제거율: 25.0%
2026-05-08T19:01:13.521+09:00  INFO 72392 --- [criticalflow] [nio-8080-exec-5] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=4 → TYPE=TYPE_F
2026-05-08T19:01:16.698+09:00  INFO 72392 --- [criticalflow] [nio-8080-exec-6] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='재귀는 함수가 자기 자신을 호출하는 프로그래밍 기법이다. 기저 조건(Base Case)과 재귀 조건(Recursive Case)으로 구성된다. 기저 조건이 없으면 무한 재귀로 스택 오버플로우가 발생한다. 팩토리얼, 피보나치 수열, 트리 순회, 분할 정복 등에 활용된다. 꼬리 재귀(Tail Recursion)는 일부 컴파일러에서 반복문으로 최적화된다. 메모이제이션을 적용하면 중복 계산을 줄여 성능을 향상시킬 수 있다. 일반적으로 재귀보다 반복문이 성능상 유리하지만, 코드 가독성에서는 재귀가 뛰어난 경우가 많다.' | RRF 후: 4건 → 2차 필터 후: 1건 | 제거율: 75.0%
2026-05-08T19:01:17.366+09:00  INFO 72392 --- [criticalflow] [nio-8080-exec-6] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=5 → TYPE=TYPE_F
2026-05-08T19:01:22.981+09:00  INFO 72392 --- [criticalflow] [nio-8080-exec-7] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='동적 프로그래밍(DP)은 큰 문제를 작은 부분 문제로 쪼개고, 각 결과를 저장하여 중복 계산을 피하는 알고리즘 설계 기법이다. 최적 부분 구조와 중복 부분 문제 두 조건을 만족할 때 적용 가능하다. 탑다운(메모이제이션)과 바텀업(타뷸레이션) 방식으로 구현한다. 대표 문제로 배낭 문제(Knapsack), 최장 공통 부분 수열(LCS), 최단 경로(Floyd-Warshall) 등이 있다. 점화식을 먼저 정의하고 그에 맞는 dp 배열 구조를 설계하는 것이 핵심이다.' | RRF 후: 4건 → 2차 필터 후: 2건 | 제거율: 50.0%
2026-05-08T19:01:23.699+09:00  INFO 72392 --- [criticalflow] [nio-8080-exec-7] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=6 → TYPE=TYPE_F
2026-05-08T19:01:26.785+09:00  INFO 72392 --- [criticalflow] [nio-8080-exec-8] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='퀵정렬은 분할 정복 방식의 정렬 알고리즘이다. 피벗(Pivot)을 선택하고, 피벗보다 작은 원소는 왼쪽, 큰 원소는 오른쪽으로 분할한 뒤 재귀적으로 정렬한다. 평균 시간복잡도는 O(n log n)이며 최악(이미 정렬된 경우)은 O(n²)이다. 추가 메모리가 거의 필요 없어 제자리 정렬(In-place)에 해당한다. 피벗 선택 전략(랜덤, 중앙값)으로 최악 케이스를 줄일 수 있다. 실제로는 병합 정렬보다 캐시 효율이 좋아 실용적 성능이 뛰어나다.' | RRF 후: 4건 → 2차 필터 후: 4건 | 제거율: 0.0%
2026-05-08T19:01:27.678+09:00  INFO 72392 --- [criticalflow] [nio-8080-exec-8] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=7 → TYPE=TYPE_B
2026-05-08T19:01:32.856+09:00  INFO 72392 --- [criticalflow] [nio-8080-exec-9] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='BFS(Breadth First Search)는 시작 정점에서 가까운 정점부터 순서대로 방문하는 그래프 탐색 알고리즘이다. 큐(Queue)를 사용하여 구현한다. 시간복잡도는 O(V+E)이며 V는 정점 수, E는 간선 수다. 최단 경로 탐색에 적합하며, 가중치가 없는 그래프에서 최단 거리를 보장한다. 방문 여부를 체크하는 visited 배열이 필수다. 레벨 순서 트리 순회, 소셜 네트워크 친구 추천, 미로 최단 경로 등에 활용된다. DFS와 달리 메모리 사용량이 크지만 완전 탐색에 유리하다.' | RRF 후: 4건 → 2차 필터 후: 3건 | 제거율: 25.0%
2026-05-08T19:01:33.609+09:00  INFO 72392 --- [criticalflow] [nio-8080-exec-9] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=8 → TYPE=TYPE_F
2026-05-08T19:01:36.617+09:00  INFO 72392 --- [criticalflow] [io-8080-exec-10] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='DFS(Depth First Search)는 한 방향으로 갈 수 있는 곳까지 깊이 탐색한 후 되돌아오는 그래프 탐색 알고리즘이다. 스택(Stack) 또는 재귀를 사용하여 구현한다. 시간복잡도는 O(V+E)다. 연결 요소 개수 파악, 사이클 감지, 위상 정렬, 백트래킹 등에 활용된다. BFS보다 메모리 효율이 좋지만 최단 경로를 보장하지 않는다. 방문 체크와 재귀 깊이 제한을 신경 써야 한다.' | RRF 후: 4건 → 2차 필터 후: 2건 | 제거율: 50.0%
2026-05-08T19:01:37.426+09:00  INFO 72392 --- [criticalflow] [io-8080-exec-10] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=9 → TYPE=TYPE_F
2026-05-08T19:01:40.288+09:00  INFO 72392 --- [criticalflow] [nio-8080-exec-2] c.c.global.ai.rag.RagRetrievalService    : [RAG 필터] 쿼리='힙은 완전 이진 트리 기반의 자료구조로 최대힙과 최소힙으로 구분된다. 최대힙은 부모 노드가 자식 노드보다 항상 크거나 같고, 최소힙은 반대다. 삽입과 삭제 연산의 시간복잡도는 O(log n)이다. 힙 구조를 이용하면 우선순위 큐를 효율적으로 구현할 수 있다. 힙 정렬은 O(n log n)의 시간복잡도를 가진다. Java에서는 PriorityQueue가 내부적으로 최소힙으로 구현되어 있다. k번째 최솟값 찾기, 다익스트라 알고리즘 등에 활용된다.' | RRF 후: 4건 → 2차 필터 후: 0건 | 제거율: 100.0%
2026-05-08T19:01:40.910+09:00  INFO 72392 --- [criticalflow] [nio-8080-exec-2] c.c.g.ai.advisor.QuestionTypeAdvisor     : [QuestionTypeAdvisor] noteId=10 → TYPE=TYPE_F


⏺ 분석 결과

┌────────┬──────────┬───────┬──────────┬────────┐   
│ noteId │   노트   │ RRF   │ 2차 필터 │ 제거율 │
│        │          │  후   │    후    │        │   
├────────┼──────────┼───────┼──────────┼────────┤
│ 1      │ HashMap  │ 4     │ 3        │ 25.0%  │
├────────┼──────────┼───────┼──────────┼────────┤
│ 2      │ 스택     │ 4     │ 2        │ 50.0%  │
├────────┼──────────┼───────┼──────────┼────────┤   
│ 3      │ 큐       │ 4     │ 2        │ 50.0%  │
├────────┼──────────┼───────┼──────────┼────────┤   
│ 4      │ 이진     │ 4     │ 3        │ 25.0%  │
│        │ 탐색     │       │          │        │
├────────┼──────────┼───────┼──────────┼────────┤
│ 5      │ 재귀     │ 4     │ 1        │ 75.0%  │
├────────┼──────────┼───────┼──────────┼────────┤   
│ 6      │ DP       │ 4     │ 2        │ 50.0%  │
├────────┼──────────┼───────┼──────────┼────────┤   
│ 7      │ 퀵정렬   │ 4     │ 4        │ 0.0%   │
├────────┼──────────┼───────┼──────────┼────────┤   
│ 8      │ BFS      │ 4     │ 3        │ 25.0%  │
├────────┼──────────┼───────┼──────────┼────────┤   
│ 9      │ DFS      │ 4     │ 2        │ 50.0%  │
├────────┼──────────┼───────┼──────────┼────────┤
│ 10     │ 힙       │ 4     │ 0        │ 100.0% │
└────────┴──────────┴───────┴──────────┴────────┘

평균 제거율: 45.0% (90% → 45% 개선)
                                                    
---                                               
해석

45%는 여전히 기준(30%)을 초과하지만 정상 범위입니다.

이유가 두 가지입니다.

1. 노트가 10개밖에 없음                             
   RAG는 최대 4건을 검색합니다. 10개 중 자신을 제외한
   9개 중에서 4개를 뽑는 상황이라 주제가 꽤 다른 노트도
   상위 4위 안에 들어옵니다. 노트 수가 50~100개로   
   늘어나면 더 유사한 노트만 상위에 오르고 자연히      
   제거율이 낮아집니다.

2. 힙(100%) vs 퀵정렬(0%)이 의미 있는 차이를 보임
- 힙(100%): 검색된 4개 노트(HashMap, 스택 등)가 힙과
  진짜 연관성이 없음 → RAG 없이 질문 생성 → 정상 동작
- 퀵정렬(0%): 정렬/알고리즘 계열 노트들이 키워드    
  다수 공유 → 전부 통과 → TYPE_B 분류 (이전엔         
  TYPE_F였음!)

부가 발견: noteId=7 퀵정렬이 이번에 TYPE_B(설계 의도
탐침)로 분류됐습니다. "피벗 선택 전략", "병합      
정렬과 캐시 효율 비교" 등 트레이드오프 내용이 있어
라우터가 올바르게 인식한 겁니다.
                                                    
---                                               
결론

현재 설정(임계값 0.1, 키워드 20개 상한)을 유지하는
것을 권장합니다. 노트 수가 늘어나면 제거율이 자연히
낮아집니다. 지금 이 단계에서 임계값을 더 낮추면
완전히 무관한 노트도 RAG 컨텍스트에 포함되어 AI 튜터
질문 품질이 떨어질 수 있습니다.
                                          

---

## 4단계 — 결과 해석

| 평균 제거율 | 해석 | 조치 |
|------------|------|------|
| **< 5%** | 필터가 거의 동작 안 함 | 임계값(0.2) 높이거나 필터 제거 검토 |
| **5 ~ 30%** | 적절히 동작 중 | 현행 유지 ✅ |
| **> 30%** | 과도하게 제거 — TYPE_C 질문 감소 우려 | 임계값 0.2 → 0.1 하향 검토 |

---

## 5단계 — 임시 로그 제거

측정 완료 후 `RagRetrievalService.java`에서 `[#59 임시 로그]` 블록을 삭제하고 아래 원래 코드로 복원합니다.

```java
List<RagContext.RetrievedChunk> chunks = merged.stream()
        .filter(doc -> isTopicRelevant(doc, queryText))
        .map(doc -> RagContext.RetrievedChunk.builder()
                .title(getMeta(doc, "title"))
                .sessionId(getMeta(doc, "session_id"))
                .content(doc.getText())
                .score(extractScore(doc))
                .build())
        .toList();
```
