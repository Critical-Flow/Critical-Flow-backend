# AI 고도화 방안

> 노트는 두 가지 형태로 작성될 수 있다.
> - **혼합형**: 한국어 설명 + 코드 블록
> - **텍스트형**: 한국어 설명만 (코드 없음)
>
> 각 방안은 두 형태 모두 동작하도록 설계되어 있으며, 코드 없는 경우의 별도 고도화 방안은 [6번 섹션](#6-코드-없는-텍스트-전용-노트-고도화)에서 다룬다.

---

## 1. 한국어 + 코드 혼합 노트 전처리

### 현재 문제

`text-embedding-3-small`은 한국어를 지원하지만, 노트 전체를 하나의 문자열로 임베딩하면 한국어 설명과 코드가 뭉쳐서 **벡터가 두 도메인의 평균**이 된다. 코드 블록이 많은 노트는 의미론적 유사도가 희석된다.

```
# 재귀 함수 (Korean)
재귀란 함수가 자기 자신을 호출하는 방식이다.

\`\`\`java
public int factorial(int n) {
    if (n == 0) return 1;
    return n * factorial(n - 1);
}
\`\`\`
→ 한국어 의미 + 코드 식별자가 혼합된 하나의 벡터로 압축됨
```

### 개선 방향: 임베딩 전 전처리

노트를 임베딩하기 전에 **코드 블록을 추출하고 주석/식별자만 남긴 텍스트로 변환**한다.

```java
// NotePreprocessor.java (신규)
public class NotePreprocessor {

    private static final Pattern CODE_BLOCK = Pattern.compile("```(\\w+)?\\n([\\s\\S]*?)```");

    public String preprocessForEmbedding(String markdown) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = CODE_BLOCK.matcher(markdown);
        int last = 0;

        while (matcher.find()) {
            // 코드 블록 이전 한국어 설명 유지
            result.append(markdown, last, matcher.start());

            // 코드 블록 → 의미 있는 식별자/주석만 추출
            String lang = matcher.group(1) != null ? matcher.group(1) : "";
            String code = matcher.group(2);
            result.append("[")
                  .append(lang.isEmpty() ? "code" : lang)
                  .append(": ")
                  .append(extractIdentifiers(code))
                  .append("] ");
            last = matcher.end();
        }
        result.append(markdown.substring(last));
        return result.toString();
    }

    private String extractIdentifiers(String code) {
        // 주석(// /* */) + 메서드명/변수명(camelCase, snake_case) 추출
        return Arrays.stream(code.split("[^a-zA-Z가-힣_]+"))
                .filter(token -> token.length() > 2)
                .distinct()
                .collect(Collectors.joining(" "));
    }
}
```

**적용 위치:** `NoteEmbeddingService.embed()` 호출 전 전처리

```java
// NoteEmbeddingService.embed() 수정
String processedContent = preprocessor.preprocessForEmbedding(note.getContent());
Document document = new Document(documentId, processedContent, metadata);
```

**효과:** "factorial", "recursive", "base case" 같은 코드 핵심 개념어가 벡터에 반영되어 유사 주제 노트 검색 정확도 향상

---

## 2. 메타데이터 자동 추출 — 프로그래밍 언어 + 토픽 태그

### 현재 메타데이터

```
note_id, session_id, user_id, title, created_at
```

언어나 주제 정보가 없어서 "Java 재귀 노트"와 "Python 재귀 노트"를 구분하지 못한다.

### 개선 방향: 저장 시 자동 추출

```java
// NoteMetadataExtractor.java (신규)
public class NoteMetadataExtractor {

    private static final Pattern CODE_LANG = Pattern.compile("```(java|python|javascript|kotlin|sql|bash|go|typescript)", Pattern.CASE_INSENSITIVE);

    // 코드 블록 언어 태그 추출
    public List<String> extractLanguages(String markdown) {
        return CODE_LANG.matcher(markdown).results()
                .map(r -> r.group(1).toLowerCase())
                .distinct()
                .toList();
    }
}
```

**메타데이터에 추가:**

```java
Map.of(
    "note_id",    note.getNoteId().toString(),
    "user_id",    note.getUserId().toString(),
    "title",      note.getTitle(),
    "languages",  String.join(",", extractor.extractLanguages(note.getContent())),  // 추가
    "created_at", note.getCreatedAt().toString()
)
```

**활용:**

```java
// 같은 언어 노트만 검색하는 필터 (선택적 적용)
.filterExpression("user_id == '" + userId + "' && languages LIKE '%java%'")
```

---

## 3. 하이브리드 검색 — Dense + Sparse (BM25)

### 현재 방식의 한계

Dense vector 검색(의미 기반)은 개념 유사도는 잘 잡지만, **코드의 정확한 식별자/함수명 매칭**에 약하다.

```
현재 노트: "ArrayList의 get() 메서드 시간복잡도"
과거 노트: "LinkedList와 ArrayList 비교 - get() O(1) vs O(n)"

→ Dense: 유사도 0.72 (임계값 0.75 미달로 탈락 가능)
→ BM25:  "ArrayList", "get()" 정확 매칭 → 높은 점수
```

### 개선 방향: BM25 병렬 검색 후 결합

ChromaDB 1.0에서 FTS 인덱스(`#document`)가 자동 생성되므로 이를 활용할 수 있다.

```java
// RagRetrievalService 수정안
public RagContext retrieve(String queryText, Long userId, Long excludeNoteId) {

    // 1. Dense 검색 (기존)
    List<Document> denseResults = vectorStore.similaritySearch(
        SearchRequest.builder()
            .query(queryText)
            .topK(maxResults + 2)
            .similarityThreshold(similarityThreshold)
            .filterExpression("user_id == '" + userId + "' && note_id != '" + excludeNoteId + "'")
            .build()
    );

    // 2. Sparse 검색 — 코드 식별자/키워드 추출 후 BM25 검색
    String keywords = extractKeyTerms(queryText);   // 4자 이상 유의미한 토큰만
    List<Document> sparseResults = vectorStore.similaritySearch(
        SearchRequest.builder()
            .query(keywords)
            .topK(maxResults)
            .similarityThreshold(0.0)               // BM25는 threshold 비적용
            .filterExpression("user_id == '" + userId + "'")
            .build()
    );

    // 3. RRF(Reciprocal Rank Fusion)로 두 결과 통합
    return mergeWithRRF(denseResults, sparseResults, maxResults);
}
```

**RRF 통합 방식:**

```java
// 두 순위 리스트를 점수 기반으로 합산 (k=60은 관례값)
private List<Document> mergeWithRRF(List<Document> dense, List<Document> sparse, int limit) {
    Map<String, Double> scores = new HashMap<>();
    int k = 60;

    for (int i = 0; i < dense.size(); i++) {
        String id = dense.get(i).getId();
        scores.merge(id, 1.0 / (k + i + 1), Double::sum);
    }
    for (int i = 0; i < sparse.size(); i++) {
        String id = sparse.get(i).getId();
        scores.merge(id, 1.0 / (k + i + 1), Double::sum);
    }

    return scores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(limit)
            .map(e -> findDocById(dense, sparse, e.getKey()))
            .filter(Objects::nonNull)
            .toList();
}
```

---

## 4. 코드 전용 질문 타입 추가 — TYPE E

### 현재 TYPE A~D의 한계

현재 질문 전략은 개념 설명 중심이다. 노트에 코드가 포함될 경우 코드 자체를 겨냥한 질문 타입이 없다.

### 개선 방향: 시스템 프롬프트에 TYPE E 추가

```
TYPE E — Code Behavior Probe
  When: the note contains a code block (```language ... ```)
  Goal: check whether the learner understands what the code actually does
        vs. what they think it does.
  Template options:
    - "이 코드의 시간복잡도는 어떻게 되나요? 왜 그렇게 생각하나요?"
    - "이 코드에서 [특정 라인]이 없다면 어떤 일이 생길까요?"
    - "이 함수에 [엣지케이스 입력]을 넣으면 어떻게 동작할까요?"
  Constraint: 절대 올바른 코드를 제시하거나 수정해주지 말 것 (LAW 1)
```

**코드 블록 감지 조건 추가:**

```java
// AiTutorService에서 시스템 프롬프트 변수 치환 시
boolean hasCodeBlock = note.getContent().contains("```");
// → {has_code} 변수로 프롬프트에 전달
// → TYPE E 활성화 여부를 AI가 판단하도록
```

---

## 5. 세션 종료 후 요약 임베딩 — 장기 기억

### 현재 방식의 한계

개별 노트만 임베딩되어 있어서 **"이 학생이 특정 개념을 얼마나 어려워했는가"** 같은 학습 패턴 정보가 RAG에 없다.

### 개선 방향: 세션 종료 시 대화 요약 생성 → 임베딩

```java
// SessionSummaryService.java (신규)
@Service
public class SessionSummaryService {

    public void summarizeAndEmbed(Long conversationId) {
        List<AiMessage> messages = aiMessageRepository.findByConversationId(conversationId);
        AiConversation conv = aiConversationRepository.findById(conversationId).orElseThrow();

        // LLM으로 대화 요약 생성
        String summaryPrompt = """
            아래 학습 대화를 3줄 이내로 요약하라.
            형식: [학습 주제] | [학습자가 이해한 부분] | [아직 불명확한 부분]
            %s
            """.formatted(formatMessages(messages));

        String summary = chatModel.call(summaryPrompt);

        // 요약을 별도 Document로 ChromaDB에 저장
        Document summaryDoc = new Document(
            "summary-" + conversationId,
            summary,
            Map.of(
                "type",           "session_summary",
                "user_id",        conv.getUserId().toString(),
                "note_id",        conv.getNoteId().toString(),
                "conversation_id", conversationId.toString(),
                "created_at",     LocalDateTime.now().toString()
            )
        );
        vectorStore.add(List.of(summaryDoc));
    }
}
```

**호출 시점:** 세션 summaryMode 진입 시 (`questionCount >= MAX_QUESTIONS`) 또는 세션 명시적 종료 시

**RAG 검색에서 요약도 포함:**

```java
// RagRetrievalService에서
// type == "session_summary" 인 Document도 함께 검색
// → 프롬프트에서 "과거 세션 요약"과 "과거 노트"를 구분해서 주입
```

---

---

## 6. 코드 없는 텍스트 전용 노트 고도화

### 기존 방안 1~5의 코드 없을 때 동작

코드 블록이 없어도 모든 방안은 정상 동작한다. 코드 관련 로직은 조건부로 처리되어 코드 블록이 없으면 자동으로 스킵된다.

| 방안 | 코드 없을 때 동작 |
|------|-----------------|
| 1. 전처리 | 코드 블록 미감지 → 원본 한국어 텍스트 그대로 임베딩 |
| 2. 메타데이터 추출 | `languages` 필드 빈 값으로 저장, 오류 없음 |
| 3. 하이브리드 검색 | 한국어 키워드로 BM25 동작 (코드 식별자 없이도 유효) |
| 4. TYPE E 질문 | `hasCodeBlock = false` → TYPE E 비활성, TYPE A~D만 사용 |
| 5. 세션 요약 임베딩 | 노트 형식 무관하게 동작 |

---

### 텍스트 전용 노트 전용 고도화

#### 6-1. 한국어 단어 길이 기준 완화 — `isTopicRelevant()` 개선

현재 2차 필터는 **4자 초과** 키워드만 유효 단어로 인정한다.

```java
// 현재
long significant = Arrays.stream(keywords).filter(k -> k.length() > 3).count();
```

영어는 4자 이하 단어가 대부분 불용어(`the`, `and`, `is`)지만, **한국어는 2~3자 단어도 핵심 개념어**다.

```
"스택" (2자) → 탈락
"큐"   (1자) → 탈락
"그래프" (3자) → 탈락
"재귀"  (2자) → 탈락
```

**개선 방향:**

```java
// RagRetrievalService.isTopicRelevant() 수정
private boolean isTopicRelevant(Document doc, String queryText) {
    String[] keywords = queryText.toLowerCase().split("\\s+");
    String content = doc.getText().toLowerCase();

    // 한국어 포함 여부 감지
    boolean hasKorean = queryText.chars().anyMatch(c -> c >= 0xAC00 && c <= 0xD7A3);
    int minLength = hasKorean ? 1 : 3;  // 한국어: 1자 이상, 영어: 3자 이상

    long significant = Arrays.stream(keywords).filter(k -> k.length() > minLength).count();
    if (significant == 0) return true;

    long matched = Arrays.stream(keywords)
            .filter(k -> k.length() > minLength)
            .filter(content::contains)
            .count();

    return (double) matched / significant >= 0.2;
}
```

---

#### 6-2. 마크다운 헤더 기반 구조 인식

텍스트 전용 노트는 코드 대신 **마크다운 헤더로 개념을 구조화**한다.

```markdown
# 스택 자료구조
## 정의
## 시간복잡도
## 활용 예시
```

헤더 텍스트를 메타데이터에 추가하면 임베딩 없이도 주제 필터링이 가능하다.

```java
// NoteMetadataExtractor에 추가
public List<String> extractHeaders(String markdown) {
    return Arrays.stream(markdown.split("\n"))
            .filter(line -> line.startsWith("#"))
            .map(line -> line.replaceAll("^#+\\s*", "").trim())
            .filter(h -> !h.isEmpty())
            .toList();
}
```

```java
// 메타데이터에 추가
"headers", String.join(",", extractor.extractHeaders(note.getContent()))
```

**활용:** `headers` 필드에서 주제어를 직접 읽어 RAG 노이즈 필터 보조

---

#### 6-3. 텍스트 전용 노트의 질문 전략 보완 — TYPE F 추가

코드 없는 개념 설명 노트에서 AI는 TYPE A(개념 정의)와 TYPE D(엣지케이스)에 집중하게 된다. 그러나 순수 이론 노트는 **"왜 이렇게 정의되는가"**, **"어떤 상황에서 쓰는가"** 같은 맥락 질문이 더 효과적이다.

**시스템 프롬프트에 TYPE F 추가:**

```
TYPE F — Contextual Application Probe (텍스트 전용 노트 특화)
  When: the note contains no code blocks and is a conceptual/theoretical note.
  Goal: check whether the learner can apply the concept to a real situation,
        not just recite the definition.
  Template options:
    - "[개념]이 실제로 어떤 문제를 해결하기 위해 등장했나요?"
    - "지금까지 개발하거나 공부하면서 [개념]이 필요했던 순간을 떠올릴 수 있나요?"
    - "[개념 A]와 [개념 B]는 어떤 상황에서 각각 선택하나요?"
```

**조건 변수 추가:**

```java
// AiTutorService에서
boolean hasCodeBlock = note.getContent().contains("```");
// {has_code} = false → TYPE F 후보로 활성화
```

---

---

## 7. 질문 타입 사전 라우팅 — Custom Advisor

### 현재 문제

현재 `tutor-system.st`에 TYPE A~D(~E, F) 설명이 전부 포함되어 있고, **LLM이 매 턴마다 모든 TYPE을 읽은 뒤 하나를 선택**한다.

```
현재: [TYPE A 설명 + TYPE B 설명 + TYPE C 설명 + TYPE D 설명] → LLM 판단 → 질문 생성
개선: [TYPE B 설명만]  →  LLM은 선택 없이 바로 질문 생성
```

TYPE 설명 전체 약 200 토큰 → 선택된 1개 약 50 토큰으로 절감.

---

### 왜 Custom Advisor인가

**LangGraph4j**는 LangChain4j 기반이라 Spring AI와 인터페이스가 다르다.
두 프레임워크를 공존시키면 OpenAI 클라이언트, Chroma 커넥션이 각각 2개씩 생기고 설정 관리가 복잡해진다.

**Spring AI의 `CallAroundAdvisor`** 는 LLM 호출 직전에 프롬프트를 가로채서 수정할 수 있어, 기존 Spring AI 스택을 그대로 유지하면서 라우팅을 구현할 수 있다.

---

### 구조

```
AiTutorService.respond()
    │
    ▼
ChatClient 호출 (Advisor 체인 통과)
    │
    ▼
QuestionTypeAdvisor.aroundCall()
    │
    ▼
SemanticQuestionTypeRouter.route()    ← 임베딩 기반 TYPE 결정
    │
    │  현재 노트 벡터 vs TYPE별 시드 벡터 cosine similarity 비교
    │
    ├── TYPE_A 유사도: 0.61
    ├── TYPE_B 유사도: 0.83  ← 최고 → TYPE B 선택
    ├── TYPE_C 유사도: 0.54  (RAG 비어있으면 후보 제외)
    ├── TYPE_D 유사도: 0.48
    ├── TYPE_E 유사도: 0.71
    └── TYPE_F 유사도: 0.59
    │
    ▼
선택된 TYPE 설명만 시스템 프롬프트에 주입
    │
    ▼
LLM 호출 → 질문 생성
```

---

### 왜 임베딩 기반인가 — 하드코딩과 비교

| 항목 | 하드코딩 규칙 | 임베딩 기반 (시맨틱 라우팅) |
|------|-------------|--------------------------|
| 정확도 | 규칙 범위 내에서만 정확 | 노트 내용 의미 기반으로 더 정확 |
| "설계 이유 없는" 코드 노트 | TYPE E로 고정 | 내용에 따라 TYPE A/B도 선택 가능 |
| 시드 문장 수정 | 코드 변경 필요 | 시드 문장만 수정 |
| 추가 LLM 호출 | 없음 | 없음 (임베딩만 사용) |
| 추가 비용 | 없음 | 노트 임베딩 1회 (약 $0.00002) |
| 시드 임베딩 초기화 | 불필요 | 앱 시작 시 1회만 수행 |

이미 `text-embedding-3-small`이 프로젝트에 있어 추가 의존성 없이 구현 가능.

---

### 구현 방향

**1. QuestionType Enum 정의**

```java
public enum QuestionType {
    TYPE_A,  // 개념 정의 확인
    TYPE_B,  // 설계 의도 탐침
    TYPE_C,  // 과거 학습 연계 (Spaced Recall)
    TYPE_D,  // 심화 탐구 (엣지케이스)
    TYPE_E,  // 코드 동작 탐침
    TYPE_F   // 실제 적용 맥락 탐침 (텍스트 전용)
}
```

**2. QuestionTypeAdvisor — LLM 호출 전 프롬프트 교체**

```java
@Component
@RequiredArgsConstructor
public class QuestionTypeAdvisor implements CallAroundAdvisor {

    private final SemanticQuestionTypeRouter router;
    private final QuestionTypePromptProvider promptProvider;

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest req, CallAroundAdvisorChain chain) {
        StudyNote note    = (StudyNote) req.adviseContext().get("note");
        RagContext rag    = (RagContext) req.adviseContext().get("ragContext");
        int questionCount = (int)       req.adviseContext().get("questionCount");

        QuestionType type = router.route(note, rag, questionCount);

        AdvisedRequest modified = req.mutate()
                .systemText(promptProvider.inject(req.systemText(), type))
                .build();

        return chain.nextAroundCall(modified);
    }

    @Override
    public String getName() { return "QuestionTypeAdvisor"; }

    @Override
    public int getOrder() { return 0; }
}
```

**3. SemanticQuestionTypeRouter — 임베딩 기반 라우팅**

```java
@Component
@RequiredArgsConstructor
public class SemanticQuestionTypeRouter {

    private final EmbeddingModel embeddingModel;

    // TYPE별 대표 시드 문장 — "이 TYPE이 적합한 상황" 설명
    private static final Map<QuestionType, List<String>> TYPE_SEEDS = Map.of(
        QuestionType.TYPE_A, List.of(
            "새로운 개념이나 용어가 처음 등장한 노트",
            "정의 없이 용어만 나열된 내용",
            "개념을 자신의 말로 설명하지 못할 것 같은 내용"
        ),
        QuestionType.TYPE_B, List.of(
            "특정 방식이나 접근법을 선택한 이유가 적힌 노트",
            "여러 대안 중 하나를 고른 설계 결정",
            "트레이드오프, 비교, 선택 기준이 포함된 내용"
        ),
        QuestionType.TYPE_C, List.of(
            "이전에 공부한 개념과 연결되는 내용",
            "과거 학습과 현재 주제가 관련된 노트",
            "기존 지식을 활용해 새 개념을 설명할 수 있는 내용"
        ),
        QuestionType.TYPE_D, List.of(
            "개념을 충분히 이해한 뒤 엣지케이스를 탐구할 단계",
            "가정이 깨지면 어떻게 되는지 생각해볼 만한 내용",
            "실패 케이스, 예외 상황, 경계 조건이 있는 내용"
        ),
        QuestionType.TYPE_E, List.of(
            "코드가 포함된 노트, 함수나 알고리즘 구현",
            "시간복잡도나 동작 방식을 확인해야 하는 코드",
            "특정 입력에서 코드가 어떻게 동작하는지 추적"
        ),
        QuestionType.TYPE_F, List.of(
            "이론 개념만 설명된 텍스트 노트",
            "실제 상황에 적용해보지 않은 순수 정의 위주 내용",
            "개념이 어떤 문제를 해결하는지 맥락 없이 정의만 나열"
        )
    );

    // 앱 시작 시 1회 임베딩 → 이후 재사용
    private Map<QuestionType, float[]> seedEmbeddings;

    @PostConstruct
    public void initSeedEmbeddings() {
        seedEmbeddings = new EnumMap<>(QuestionType.class);
        TYPE_SEEDS.forEach((type, seeds) ->
            seedEmbeddings.put(type, averageEmbedding(seeds)));
    }

    public QuestionType route(StudyNote note, RagContext rag, int questionCount) {
        Set<QuestionType> candidates = new HashSet<>(Set.of(QuestionType.values()));

        // TYPE C는 RAG 결과가 있을 때만 후보로 포함
        if (rag.isEmpty()) candidates.remove(QuestionType.TYPE_C);

        float[] noteVec = embed(note.getContent());

        return candidates.stream()
                .max(Comparator.comparingDouble(
                        type -> cosineSimilarity(noteVec, seedEmbeddings.get(type))))
                .orElse(QuestionType.TYPE_A);
    }

    private float[] averageEmbedding(List<String> sentences) {
        List<float[]> vecs = sentences.stream().map(this::embed).toList();
        float[] avg = new float[vecs.get(0).length];
        for (float[] v : vecs)
            for (int i = 0; i < v.length; i++) avg[i] += v[i] / vecs.size();
        return avg;
    }

    private float[] embed(String text) {
        return embeddingModel.embed(text).toFloatArray();
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
```

**4. tutor-system.st 변경**

```
// 변경 전: 모든 TYPE 설명 하드코딩 (~200 토큰)
TYPE A — Concept Verification ...
TYPE B — Design Intent Probe ...
TYPE C — Spaced Recall Quiz ...
TYPE D — Deep Thinking Probe ...

// 변경 후: 플레이스홀더로 교체 (~50 토큰)
{selected_question_type}
```

**5. AiTutorService에서 컨텍스트 주입**

```java
chatClient.prompt()
    .advisors(advisor -> advisor
        .param("note", note)
        .param("ragContext", ragContext)
        .param("questionCount", questionCount))
    .call();
```

---

### 수정이 필요한 파일

| 파일 | 변경 내용 |
|------|-----------|
| `tutor-system.st` | TYPE A~F 설명 제거, `{selected_question_type}` 플레이스홀더 추가 |
| `AiTutorService.java` | `ChatClient` Advisor 컨텍스트 주입 방식으로 변경 |
| `QuestionTypeAdvisor.java` | 신규 — `CallAroundAdvisor` 구현 |
| `SemanticQuestionTypeRouter.java` | 신규 — 임베딩 기반 시맨틱 라우팅 |
| `QuestionTypePromptProvider.java` | 신규 — TYPE별 프롬프트 텍스트 관리 |

---

## 8. 질문 타입 라우팅 학습 데이터 축적

### 배경

질문 타입(TYPE A~F) 선택 알고리즘을 장기적으로 Fine-tuned 모델로 고도화하기 위해, LLM이 결정한 TYPE을 DB에 저장해 학습 데이터를 점진적으로 축적한다.

자세한 전략은 [question-type-routing.md](question-type-routing.md) 참조.

### 구현: `AiConversation`에 `question_type` 컬럼 추가

별도 테이블을 만들지 않고 기존 `AiConversation`에 컬럼을 추가한다.
TYPE은 "이 대화에서 어떤 방식으로 질문할지"이므로 대화 엔티티에 귀속되는 것이 의미상 자연스럽다.

```java
// AiConversation.java — 추가
@Enumerated(EnumType.STRING)
@Column(name = "question_type")
private QuestionType questionType;  // TYPE_A ~ TYPE_F

public enum QuestionType {
    TYPE_A, TYPE_B, TYPE_C, TYPE_D, TYPE_E, TYPE_F
}
```

```sql
-- 마이그레이션
ALTER TABLE ai_conversation ADD COLUMN question_type VARCHAR(10);
```

### Fine-tuning 데이터 추출 쿼리

```sql
SELECT
    n.content  AS note_content,
    c.question_type AS label
FROM ai_conversation c
JOIN study_note n ON c.note_id = n.note_id
WHERE c.question_type IS NOT NULL;
```

이 결과를 OpenAI Fine-tuning용 JSONL로 변환:

```jsonl
{"messages": [{"role": "user", "content": "노트 내용: ..."}, {"role": "assistant", "content": "TYPE_A"}]}
{"messages": [{"role": "user", "content": "노트 내용: ..."}, {"role": "assistant", "content": "TYPE_E"}]}
```

### 전환 기준

| 단계 | 조건 | 행동 |
|------|------|------|
| 1단계 | 지금 | LLM 분류 (GPT-4o-mini) + 결과 DB 저장 |
| 2단계 | 전체 누적 200~300개 | PLOS ONE 데이터 + 축적 데이터로 OpenAI Fine-tuning |
| 3단계 | Fine-tuning 완료 | `ft:gpt-4o-mini:...` 모델 ID로 교체, LLM 분류 비용 절감 |

### 수정이 필요한 파일

| 파일 | 변경 내용 |
|------|-----------|
| `AiConversation.java` | `questionType` 필드 추가 |
| `AiConversationRepository.java` | Fine-tuning 데이터 추출 쿼리 메서드 추가 |
| `QuestionTypeRouter.java` | 신규 — LLM 분류 + 규칙 기반 전처리 필터 |

---

## 고도화 우선순위

| 방안 | 노트 형태 | 난이도 | 기대 효과 | 우선순위 |
|------|----------|--------|-----------|----------|
| 1. 코드 블록 전처리 | 혼합형 | 낮음 | 임베딩 품질 향상 | 높음 |
| 2. 메타데이터 자동 추출 | 혼합형 | 낮음 | 언어별 필터링 가능 | 높음 |
| 4. TYPE E 질문 타입 | 혼합형 | 낮음 | 코드 노트 튜터링 강화 | 높음 |
| 6-1. 한국어 단어 길이 완화 | 텍스트형 | 낮음 | 2차 필터 한국어 정확도 향상 | 높음 |
| 7. Custom Advisor 질문 타입 라우팅 | 공통 | 중간 | 매 턴 토큰 약 150개 절감 | 높음 |
| 6-2. 헤더 기반 메타데이터 | 텍스트형 | 낮음 | 주제 구조 인식 | 중간 |
| 3. 하이브리드 검색 (BM25) | 공통 | 중간 | 검색 정확도 향상 | 중간 |
| 6-3. TYPE F 질문 타입 | 텍스트형 | 낮음 | 개념 적용력 검증 강화 | 중간 |
| 5. 세션 요약 임베딩 | 공통 | 높음 | 장기 학습 패턴 추적 | 낮음 |

> **권장 순서:**
> - 혼합형 노트 위주라면: 1 → 2 → 4 → 7 → 3 → 5
> - 텍스트형 노트 위주라면: 6-1 → 7 → 6-2 → 6-3 → 3 → 5
> - 두 형태 모두 사용한다면: 6-1 → 1 → 2 → 7 → 4 + 6-3 → 3 → 5
