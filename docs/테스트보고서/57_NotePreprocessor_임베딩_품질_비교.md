# #57 — NotePreprocessor 전처리 적용 전후 임베딩 품질 정량 비교

| 항목 | 내용 |
|---|---|
| 작성자 | chamingyeong |
| 작성일 | 2026-05-11 |
| 관련 요구사항 ID | REQ-AIV-011 |
| 관련 이슈/PR 번호 | #57 / PR #71 |
| 테스트 상태 | 완료 |

---

## 공통 테스트 환경

| 항목 | 값 |
|---|---|
| OS | macOS Darwin 25.4.0 |
| JDK | 21.0.6 LTS |
| Spring Boot | 3.2.4 |
| LLM 모델 | gpt-4o (temperature: 0.3) |
| 임베딩 모델 | text-embedding-3-small (OpenAI) |
| 벡터 DB | ChromaDB 1.0.0 (Testcontainers) |
| 실행 환경 | 로컬 |
| 외부 API 호출 여부 | 실제 호출 (OpenAI API) |
| 테스트 데이터셋 | 한국어 CS 노트 10개 (코드 블록 포함) |
| 문서 최초 작성일 | 2026-05-11 |
| 최종 수정일 | 2026-05-11 |

---

## 1. 진행 이유

**1-1. 발견 경위**
`NoteEmbeddingService.embed()` 내부에서 `NotePreprocessor.preprocessForEmbedding()`을 호출해 코드 블록을 `[python: fibonacci return ...]` 형태의 식별자 텍스트로 변환한 뒤 ChromaDB에 임베딩하고 있었다. 이 전처리 로직은 코드 블록의 구문 기호(`{`, `;`, `}`)가 벡터를 희석시킨다는 가설로 설계됐지만, 실제로 전처리가 유사도 검색 품질을 높이는지 수치로 확인한 적이 없었다.

**1-2. 해결하지 않을 경우 영향**
- 전처리가 효과 없다면 코드 파싱 로직이 불필요한 복잡성만 추가
- 전처리가 오히려 품질을 낮춘다면 코드 관련 노트의 검색 정밀도 저하
- 품질 기준선이 없어 향후 `NotePreprocessor` 수정 시 개선 방향 판단 불가

**1-3. 관련 요구사항**
REQ-AIV-011 — 관련 노트 검색 재현율이 측정 기준치(Recall@4 ≥ 0.7) 이상이어야 한다.

---

## 2. 측정 방법

**2-1. 측정 방식**
- [x] 수동 실험 (`NotePreprocessorEmbeddingQualityTest.java` 실행 후 관찰)

**2-2. 측정 조건**
`threshold=0.0`으로 전체 후보를 조회해 Recall@4만 비교. NotePreprocessor 적용/미적용 두 조건을 Phase 1(원본 마크다운) / Phase 2(전처리 적용)로 구분해 같은 Testcontainers ChromaDB에서 순차 실행.

**2-3. 테스트 데이터 구성**
- 데이터 출처: 직접 작성한 한국어 CS 학습 노트 (코드 블록 포함)
- 데이터 건수: 10개 (qnote-1 ~ qnote-10), 정답 쿼리 10개

| ID | 노트 제목 | 정답 쿼리 |
|----|----------|---------|
| qnote-1 | HashMap 자료구조 | HashMap put get containsKey 사용법 |
| qnote-2 | 재귀 함수 팩토리얼 | 재귀 호출 팩토리얼 기저 조건 구현 |
| qnote-3 | 이진 탐색 알고리즘 | 정렬된 배열 이진 탐색 left right mid 구현 |
| qnote-4 | SQL JOIN 쿼리 | SQL INNER JOIN LEFT JOIN 테이블 조인 |
| qnote-5 | 버블 정렬 알고리즘 | 버블 정렬 swap 인접 비교 알고리즘 구현 |
| qnote-6 | 스프링 의존성 주입 | 스프링 Autowired 빈 의존성 주입 Service |
| qnote-7 | 동적 프로그래밍 메모이제이션 | 메모이제이션 dp 피보나치 동적 프로그래밍 |
| qnote-8 | BFS 그래프 탐색 | BFS 너비 우선 탐색 큐 Queue LinkedList |
| qnote-9 | 스택 자료구조 | 스택 LIFO push pop peek 괄호 매칭 |
| qnote-10 | 자바 예외 처리 | 자바 예외 처리 try catch finally IOException |

- 데이터 특성: Python·Java 코드 블록이 포함된 CS 노트
- 정답 레이블 여부: 있음 (노트별 1:1 정답 쿼리 수동 작성)

**2-4. 측정 기준 및 도구**
- 측정 지표: Recall@4 (Top-4 결과 내 정답 노트 포함 비율)
- 측정 도구: `NotePreprocessorEmbeddingQualityTest.java` 내 JUnit 단언문
- 성공 기준: 전처리 미적용 Recall@4 − 전처리 적용 Recall@4 ≥ 10%p

**2-5. 측정 반복 횟수**
1회 (임베딩 모델이 결정론적이므로 동일 입력은 동일 결과)

**2-6. 이 방식을 선택한 이유**
임베딩 품질은 벡터 공간에서 실제 문서 간 유사도 점수를 비교해야 측정 가능해 단위 테스트로는 검증할 수 없다. 실제 OpenAI API + ChromaDB가 필요한 수동 실험을 선택했다.

**2-7. 테스트 노트 실제 내용 (코드 블록 포함)**

아래는 ChromaDB에 임베딩한 10개 노트의 실제 내용이다.

<details>
<summary>qnote-1 — HashMap 자료구조</summary>

```markdown
## HashMap 자료구조
HashMap은 Java에서 키-값 쌍을 저장하는 자료구조다.

```java
import java.util.HashMap;
import java.util.Map;

Map<String, Integer> map = new HashMap<>();
map.put("apple", 3);
int count = map.getOrDefault("banana", 0);
boolean has = map.containsKey("apple");
map.remove("apple");
```

내부적으로 배열 + 연결 리스트 기반 해시 테이블. Java 8 이후 버킷 길이 ≥ 8이면 Red-Black Tree로 전환. 평균 O(1), 최악 O(log n).
```
</details>

<details>
<summary>qnote-2 — 재귀 함수 팩토리얼</summary>

```markdown
## 재귀 함수와 팩토리얼
재귀는 함수가 자기 자신을 호출하는 기법이다.

```java
public static long factorial(int n) {
    if (n <= 1) return 1;          // 기저 조건(base case)
    return n * factorial(n - 1);   // 재귀 조건
}
```

기저 조건 없으면 StackOverflowError. 메모이제이션으로 피보나치 최적화 가능.
```
</details>

<details>
<summary>qnote-3 — 이진 탐색 알고리즘</summary>

```markdown
## 이진 탐색(Binary Search)
정렬된 배열에서 O(log n)으로 목표값을 찾는다.

```java
public static int binarySearch(int[] arr, int target) {
    int left = 0, right = arr.length - 1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (arr[mid] == target) return mid;
        if (arr[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    return -1;
}
```

`mid = (left+right)/2` 대신 `left + (right-left)/2` 사용 이유: 정수 오버플로우 방지.
```
</details>

<details>
<summary>qnote-7 — 동적 프로그래밍 메모이제이션 (허브 오염의 원인)</summary>

```java
import java.util.HashMap;

public class DynamicProgramming {
    private Map<Integer, Long> memo = new HashMap<>();

    public long fibonacci(int n) {
        if (memo.containsKey(n)) return memo.get(n);
        if (n <= 1) return n;
        long result = fibonacci(n-1) + fibonacci(n-2);
        memo.put(n, result);
        return result;
    }

    public int climbStairs(int n) {
        int[] dp = new int[n + 1];
        dp[0] = 1; dp[1] = 1;
        for (int i = 2; i <= n; i++)
            dp[i] = dp[i-1] + dp[i-2];
        return dp[n];
    }
}
```

전처리 후 qnote-7의 임베딩 입력 텍스트:
```
[java: HashMap memo containsKey fibonacci climbStairs result DynamicProgramming]
```
→ `HashMap`(qnote-1 핵심), `containsKey`(qnote-1 핵심), `fibonacci`(qnote-2 핵심) 가 혼재 — 이 식별자들이 다른 노트와 거짓 유사도를 만든다.
</details>

**2-8. 테스트 실행 방법**

```bash
# OpenAI API 키 설정 후 실행
export OPENAI_API_KEY=sk-...

# 전처리 적용 조건 테스트 (Phase 2 — preprocessor on)
./gradlew test \
  --tests "*.NotePreprocessorEmbeddingQualityTest.phase2*" \
  -Dspring.profiles.active=test-embedding

# 전처리 미적용 조건 테스트 (Phase 1 — raw markdown)
./gradlew test \
  --tests "*.NotePreprocessorEmbeddingQualityTest.phase1*" \
  -Dspring.profiles.active=test-embedding
```

---

## 3. 1차 결과 (수정 전 — NotePreprocessor 적용 상태)

**3-1. 측정 결과**

| 측정 항목 | 결과 |
|---|---|
| Recall@4 | **40% (4/10)** |

| 쿼리 노트 | 결과 | Top-4 |
|---|---|---|
| qnote-1 (HashMap) | **MISS** | qnote-7, qnote-4, qnote-10, qnote-2 |
| qnote-2 (재귀) | **MISS** | qnote-7, qnote-10, qnote-4, qnote-5 |
| qnote-3 (이진탐색) | **MISS** | qnote-4, qnote-7, qnote-9, qnote-6 |
| qnote-4 (SQL) | HIT | qnote-4, qnote-7, qnote-9, qnote-2 |
| qnote-5 (버블정렬) | **MISS** | qnote-7, qnote-9, qnote-4, qnote-10 |
| qnote-6 (스프링 DI) | **MISS** | qnote-10, qnote-4, qnote-7, qnote-3 |
| qnote-7 (DP) | HIT | qnote-7, qnote-9, qnote-10, qnote-4 |
| qnote-8 (BFS) | **MISS** | qnote-9, qnote-7, qnote-10, qnote-2 |
| qnote-9 (스택) | HIT | qnote-9, qnote-7, qnote-4, qnote-6 |
| qnote-10 (예외처리) | HIT | qnote-10, qnote-9, qnote-7, qnote-8 |

**3-2. qnote-7 허브 오염 분포**

| 쿼리 노트 | Top-4 내 qnote-7 등장 | 예상 동작 |
|---|---|---|
| qnote-1 (HashMap) | **등장** | 미등장이어야 함 |
| qnote-2 (재귀) | **등장** | 미등장이어야 함 |
| qnote-3 (이진탐색) | **등장** | 미등장이어야 함 |
| qnote-4 (SQL) | **등장** | 미등장이어야 함 |
| qnote-5 (버블정렬) | **등장** | 미등장이어야 함 |
| qnote-6 (스프링 DI) | **등장** | 미등장이어야 함 |
| qnote-7 (DP) | 등장 (자기 자신) | 정상 |
| qnote-8 (BFS) | **등장** | 미등장이어야 함 |
| qnote-9 (스택) | 등장 | 정상 |
| qnote-10 (예외처리) | **등장** | 미등장이어야 함 |

→ qnote-7이 전체 10회 쿼리 중 **8회** Top-4 내 등장. 이 중 정상 등장 2회, 오염 등장 6회.

**3-3. 문제 증상**
qnote-7(DP/메모이제이션)이 10개 쿼리 중 6개의 Top-4에 잘못 등장하는 **허브 오염 현상** 발생. Recall@4 40%로 성공 기준(≥ 10%p 향상)을 역방향으로 위반.

**3-4. 원인 가설**
전처리 후 qnote-7의 임베딩 대상 텍스트가 다음과 같다:
```
[java: HashMap memo containsKey fibonacci climbStairs result DynamicProgramming]
```
이 식별자들은 여러 주제와 교차한다:
| 식별자 | 원래 주제 |
|--------|---------|
| `HashMap`, `containsKey` | qnote-1 (HashMap 자료구조) |
| `fibonacci` | qnote-2 (재귀) |
| `result`, `climbStairs` | 범용 변수명 — 여러 노트 |

**3-5. 원인 확정 근거**
`NotePreprocessor.preprocessForEmbedding()` 출력을 직접 확인한 결과 위 식별자 목록이 임베딩 입력으로 들어감을 확인. `text-embedding-3-small`은 코드+한국어 혼합 문서를 문맥 전체로 이해하도록 훈련돼 있어, 식별자 목록으로 압축하면 문맥 관계가 소실되고 공통 변수명이 거짓 유사도를 유발하는 것으로 확정.

---

## 4. 조치

**4-1. 수정 방향 및 근거**
`NotePreprocessor` 호출을 제거하고 원본 마크다운 그대로 임베딩. `text-embedding-3-small`은 코드+한국어 혼합 문서를 원래부터 잘 처리하도록 훈련돼 있어 전처리가 오히려 모델의 강점을 역이용하는 결과를 낳음.

**4-2. 변경 내역**

| 변경 대상 | 변경 전 | 변경 후 |
|---|---|---|
| `NoteEmbeddingService.embed()` | `notePreprocessor.preprocessForEmbedding(note.getContent())` | `note.getContent()` |
| `POST /api/notes/reembed` | 없음 | 전체 재임베딩 엔드포인트 추가 |

```java
// 변경 전
String processedContent = notePreprocessor.preprocessForEmbedding(note.getContent());

// 변경 후
String processedContent = note.getContent();  // 원본 마크다운 그대로
```

**4-3. 변경 내용 요약**
`NoteEmbeddingService`에서 `NotePreprocessor` 호출 한 줄 제거. 기존에 전처리된 벡터가 ChromaDB에 저장돼 있으므로 `POST /api/notes/reembed`로 재임베딩 필요.

---

## 5. 2차 결과 (수정 후 — NotePreprocessor 미적용 상태)

**5-1. 측정 결과**

| 측정 항목 | 결과 |
|---|---|
| Recall@4 | **100% (10/10)** |

| 쿼리 노트 | 결과 | Top-4 |
|---|---|---|
| qnote-1 (HashMap) | HIT | qnote-1, qnote-3, qnote-6, qnote-8 |
| qnote-2 (재귀) | HIT | qnote-2, qnote-7, qnote-6, qnote-10 |
| qnote-3 (이진탐색) | HIT | qnote-3, qnote-5, qnote-8, qnote-4 |
| qnote-4 (SQL) | HIT | qnote-4, qnote-3, qnote-8, qnote-9 |
| qnote-5 (버블정렬) | HIT | qnote-5, qnote-3, qnote-7, qnote-9 |
| qnote-6 (스프링 DI) | HIT | qnote-6, qnote-10, qnote-4, qnote-1 |
| qnote-7 (DP) | HIT | qnote-7, qnote-2, qnote-5, qnote-3 |
| qnote-8 (BFS) | HIT | qnote-8, qnote-3, qnote-9, qnote-5 |
| qnote-9 (스택) | HIT | qnote-9, qnote-7, qnote-3, qnote-8 |
| qnote-10 (예외처리) | HIT | qnote-10, qnote-9, qnote-6, qnote-7 |

**5-2. 수정 전후 비교**

| 측정 항목 | 수정 전 (전처리 적용) | 수정 후 (전처리 미적용) | 변화량 |
|---|---|---|---|
| Recall@4 | 40% | **100%** | **+60%p** |

**5-3. 수정이 효과적인 이유 — 논리적 분석**

**① `text-embedding-3-small`은 왜 코드 원문을 더 잘 처리하는가**

`text-embedding-3-small`은 OpenAI가 GitHub·StackOverflow·기술 문서를 포함한 대규모 코드+자연어 혼합 데이터로 훈련한 Transformer 기반 임베딩 모델이다. Transformer 어텐션 메커니즘은 입력 시퀀스 전체를 동시에 처리하며 토큰 간 상호 관계를 학습한다. 이 과정에서 다음 구조를 자연스럽게 인식한다:
- 함수 시그니처 + 파라미터 타입 + 리턴 타입의 의미 관계
- 변수 선언 → 사용 패턴이 코드의 "주제"를 나타낸다는 것
- 한국어 설명과 인접한 코드 블록이 같은 주제에 속한다는 것

**② 전처리가 모델의 강점을 파괴하는 메커니즘**

`NotePreprocessor`는 아래와 같이 코드를 식별자 목록으로 변환한다:

```java
// 원본 코드 블록
public static int binarySearch(int[] arr, int target) {
    int left = 0, right = arr.length - 1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        ...
    }
}

// 전처리 후 임베딩 입력
[java: binarySearch arr target left right mid]
```

이 변환이 파괴하는 정보:
1. **함수 관계 구조** — `left + (right - left) / 2`가 오버플로우 방지 패턴임을 알 수 없게 됨
2. **언어 문맥** — Java의 `while` 루프 패턴이 사라져 "이진 탐색 알고리즘" 의미가 약화
3. **범용 식별자의 허브 문제** — `result`, `arr`, `left`, `right`, `n`, `i`처럼 모든 알고리즘에서 공통으로 쓰이는 변수명이 노트 간 거짓 유사도를 만든다. qnote-7(DP)의 `HashMap`, `fibonacci` 식별자가 qnote-1(HashMap), qnote-2(재귀)와 높은 유사도를 만드는 것이 허브 오염의 정확한 원인

**③ Word2Vec 가설이 이 모델에서 틀린 이유**

전처리 설계의 근거는 "코드 구문 기호가 벡터를 희석시킨다"는 Word2Vec 시대의 통찰이었다. Word2Vec은 단어를 독립적으로 임베딩하므로 `{`, `;` 같은 기호가 관련 없는 단어를 같은 공간으로 끌어당길 수 있다. 그러나 `text-embedding-3-small`은:
- 서브워드 토크나이저로 `{`를 독립 토큰으로 처리해 영향을 최소화
- 어텐션 가중치로 관련 없는 토큰의 영향력을 문맥에 따라 조절
- 훈련 데이터에 코드가 대규모로 포함되어 구문 기호를 의미 있는 패턴으로 이미 학습

**5-4. 부작용 및 회귀 여부**
`NotePreprocessor.java` 파일 자체는 삭제하지 않고 유지. `NoteEmbeddingService`의 호출 제거만으로 충분하며 다른 컴포넌트에 영향 없음. 기존 ChromaDB 벡터는 `POST /api/notes/reembed` 호출로 재임베딩 완료(커밋 93973dd).

---

## 6. 결론

**6-1. 목표 달성 여부**
- [x] 달성 — Recall@4 +60%p 개선 확인 (기준 10%p 대폭 초과)

**6-2. 관련 요구사항 충족 여부**
REQ-AIV-011 충족 — 전처리 제거 후 Recall@4 100% 달성, 기준치 이상 검색 품질 확보

**6-3. 결론 및 결정 근거**

`NotePreprocessor`는 Word2Vec 시대의 설계 패턴을 최신 Transformer 임베딩 모델에 잘못 적용한 사례였다.

| 판단 근거 | 내용 |
|---|---|
| 실측 결과 | Recall@4 40% → 100%, +60%p 향상 |
| 이론적 근거 | `text-embedding-3-small`은 코드+한국어 혼합 문서를 원문 그대로 처리하도록 훈련됨 |
| 허브 오염 메커니즘 | 식별자 목록으로 압축 시 범용 변수명이 주제 간 거짓 유사도를 만듦 |
| 결정 | NotePreprocessor 호출 제거, 원본 마크다운 임베딩으로 전환 |

**6-4. 잔여 과제 및 후속 조치**
`NotePreprocessor.java` 파일이 사용되지 않는 상태로 남아 있어 향후 정리 검토 필요.

**6-5. 팀 공유 사항**
`text-embedding-3-small`은 코드·한국어 혼합 문서를 원문 그대로 임베딩하는 것이 최적. 향후 전처리 재도입 시 반드시 Recall@4 기준선(100%)과 비교 측정 후 결정할 것.
