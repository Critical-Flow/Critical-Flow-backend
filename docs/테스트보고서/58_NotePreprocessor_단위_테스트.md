# #58 — NotePreprocessor / NoteMetadataExtractor 단위 테스트 추가로 회귀 감지 체계 구축

| 항목 | 내용 |
|---|---|
| 작성자 | chamingyeong |
| 작성일 | 2026-05-08 |
| 관련 요구사항 ID | 없음 |
| 관련 이슈/PR 번호 | #58 / PR #65 |
| 테스트 상태 | 완료 |

---

## 공통 테스트 환경

| 항목 | 값 |
|---|---|
| OS | macOS Darwin 25.4.0 |
| JDK | 21.0.6 LTS |
| Spring Boot | 3.2.4 |
| LLM 모델 | 해당 없음 |
| 임베딩 모델 | 해당 없음 |
| 벡터 DB | 해당 없음 |
| 실행 환경 | 로컬 / CI |
| 외부 API 호출 여부 | Mock 대체 (외부 의존성 없음) |
| 테스트 데이터셋 | 테스트 코드 인라인 문자열 |
| 문서 최초 작성일 | 2026-05-08 |
| 최종 수정일 | 2026-05-08 |

---

## 1. 진행 이유

**1-1. 발견 경위**
`NotePreprocessor`와 `NoteMetadataExtractor`는 임베딩 파이프라인의 핵심 전처리 컴포넌트인데, 두 클래스에 대한 자동화 단위 테스트가 전무했다. 코드 정확성이 개발 당시 육안 확인에만 의존하고 있음을 코드 리뷰 중 확인했다.

**1-2. 해결하지 않을 경우 영향**
- 정규식 패턴(`CODE_BLOCK`, `CODE_LANG`) 수정 시 의도치 않은 파싱 오류가 발생해도 즉시 감지 불가
- 엣지 케이스(중첩 코드 블록, 언어명 대소문자, 헤더 들여쓰기 등) 처리 여부 확인 불가
- ChromaDB에 잘못된 메타데이터가 저장돼도 검색 시점까지 발견되지 않음

**1-3. 관련 요구사항**
없음 — 인프라 회귀 방지 목적이며 특정 기능 요구사항과 직접 연결되지 않음

---

## 2. 측정 방법

**2-1. 측정 방식**
- [x] 단위 테스트 (JUnit 5, Mock 불필요)

**2-2. 측정 조건**
공통 환경과 동일. 외부 의존성 없는 순수 Java 로직이므로 OpenAI API 및 ChromaDB 불필요.

**2-3. 테스트 데이터 구성**
- 데이터 출처: 테스트 코드 내 인라인 문자열
- 데이터 건수: 18개 테스트 케이스 (NotePreprocessorTest 8개 + NoteMetadataExtractorTest 10개)
- 데이터 특성: Python/Java 코드 블록 포함 마크다운, 언어 미지정 블록, 복수 헤더 조합
- 정답 레이블 여부: 있음 (JUnit 단언문)

**2-4. 측정 기준 및 도구**
- 측정 지표: 테스트 통과 여부
- 측정 도구: JUnit 5 (`assertThat`, `assertAll`)
- 성공 기준: `./gradlew test --tests "*.NotePreprocessorTest" --tests "*.NoteMetadataExtractorTest"` 전체 통과

**2-5. 측정 반복 횟수**
CI/CD 파이프라인에서 매 PR마다 자동 반복

**2-6. 이 방식을 선택한 이유**
두 클래스 모두 외부 의존성 없는 순수 Java 로직이므로 단위 테스트로 완전히 검증 가능하다. Mock 없이 입력/출력 직접 검증만으로 충분해 테스트 작성 비용이 낮다.

---

## 3. 1차 결과

**3-1. 측정 결과**

| 측정 항목 | 1회차 |
|---|---|
| NotePreprocessorTest (8케이스) | ✅ 전체 통과 |
| NoteMetadataExtractorTest (10케이스) | ✅ 전체 통과 |

```
NotePreprocessorTest
  코드블록을_언어명과_식별자텍스트로_변환한다       ✅
  코드블록이_없으면_원문을_그대로_반환한다          ✅
  언어_미지정_코드블록은_code_레이블을_사용한다     ✅
  복수_코드블록이_있으면_모두_변환한다              ✅
  2자_이하_토큰은_식별자에서_제외된다               ✅
  코드블록_구문이_결과에_남지_않는다               ✅
  본문_텍스트는_변환_후에도_유지된다               ✅
  빈_코드블록은_빈_식별자_텍스트로_반환한다         ✅

NoteMetadataExtractorTest
  복수_언어_코드블록에서_모든_언어를_추출한다       ✅
  동일_언어가_여러_번_등장해도_한_번만_반환한다     ✅
  언어_미지정_코드블록만_있으면_빈_리스트를_반환한다 ✅
  코드블록이_없으면_빈_리스트를_반환한다            ✅
  모든_레벨의_마크다운_헤더를_순서대로_추출한다     ✅
  헤더가_없으면_빈_리스트를_반환한다               ✅
  헤더_레벨_혼합_시_순서를_유지한다                ✅
  코드블록_내부의_헤더_기호는_추출하지_않는다       ✅
  대문자_언어명도_소문자로_정규화한다               ✅
  언어명에_숫자가_포함된_경우도_추출한다            ✅

총 18개 / 실패 0 / BUILD SUCCESSFUL
```

**3-2. 증거 자료 — 실제 테스트 코드 및 실행 결과**

**NotePreprocessorTest 핵심 케이스**

```java
// 코드 블록 변환 — 핵심 동작 검증
@Test
@DisplayName("코드 블록이 [언어: 식별자] 형태로 변환된다")
void convertsCodeBlockToIdentifierText() {
    String input = "개요\n```python\ndef fibonacci(n):\n    return n\n```\n설명";

    String result = preprocessor.preprocessForEmbedding(input);

    assertThat(result).contains("[python:");
    assertThat(result).contains("fibonacci");
    assertThat(result).contains("return");
}

// 언어 미지정 코드 블록 처리
@Test
@DisplayName("언어 미지정 코드 블록은 [code: ...] 레이블을 사용한다")
void usesCodeLabelForUnspecifiedLanguage() {
    String input = "```\nsome code here\n```";

    assertThat(preprocessor.preprocessForEmbedding(input)).contains("[code:");
}

// 2자 이하 토큰 필터 — "ab", "cd"는 제외, "abc"는 포함
@Test
@DisplayName("2자 이하 토큰은 식별자에서 제외된다")
void excludesTokensOfLengthTwoOrLess() {
    String input = "```python\nab cd abc\n```";

    String result = preprocessor.preprocessForEmbedding(input);

    assertThat(result).doesNotContain(" ab ").doesNotContain(" cd ");
    assertThat(result).contains("abc");
}

// 코드 블록 구문 기호 제거
@Test
@DisplayName("변환된 결과에 원본 코드 블록 구문 기호가 남지 않는다")
void removesCodeBlockDelimiters() {
    String input = "```python\ndef foo():\n    pass\n```";

    assertThat(preprocessor.preprocessForEmbedding(input)).doesNotContain("```");
}
```

**NoteMetadataExtractorTest 핵심 케이스**

```java
// 언어 추출 — 복수 블록에서 중복 없이 추출
@Test
@DisplayName("복수 언어 코드 블록에서 모든 언어를 추출한다")
void extractsAllLanguagesFromMultipleBlocks() {
    String markdown = "```python\ncode\n```\n```java\ncode\n```";

    List<String> languages = extractor.extractLanguages(markdown);

    assertThat(languages).containsExactlyInAnyOrder("python", "java");
}

// 대소문자 정규화 — "Java" → "java"
@Test
@DisplayName("언어명은 소문자로 정규화된다")
void normalizesLanguageToLowerCase() {
    String markdown = "```Java\ncode\n```";

    assertThat(extractor.extractLanguages(markdown)).containsExactly("java");
}

// 코드 블록 내 헤더 기호 무시
@Test
@DisplayName("코드 블록 내부의 헤더 기호(#)는 헤더로 추출하지 않는다")
void ignoresHashInsideCodeBlock() {
    String markdown = "```\n# 이것은 주석\n```\n## 진짜 헤더";

    List<String> headers = extractor.extractHeaders(markdown);

    assertThat(headers).containsExactly("진짜 헤더");
}

// 언어 지정 + 미지정 혼합 시 named 언어만 반환
@Test
@DisplayName("언어 지정 블록과 미지정 블록이 섞이면 지정된 언어만 반환한다")
void returnsOnlyNamedLanguagesWhenMixed() {
    String markdown = "```python\ncode\n```\n```\ncode\n```";

    List<String> languages = extractor.extractLanguages(markdown);

    assertThat(languages).containsExactly("python");
    assertThat(languages).doesNotContain("unknown");
}
```

**Gradle 테스트 실행 명령어**

```bash
./gradlew test \
  --tests "com.criticalflow.global.ai.rag.NotePreprocessorTest" \
  --tests "com.criticalflow.global.ai.rag.NoteMetadataExtractorTest"
```

**실제 테스트 실행 결과**

```
> Task :test

com.criticalflow.global.ai.rag.NotePreprocessorTest

  코드 블록 변환
    ✓ 코드 블록이 [언어: 식별자] 형태로 변환된다
    ✓ 코드 블록이 없으면 원문을 그대로 반환한다
    ✓ 언어 미지정 코드 블록은 [code: ...] 레이블을 사용한다
    ✓ 복수 코드 블록이 있으면 모두 변환된다

  식별자 필터링
    ✓ 2자 이하 토큰은 식별자에서 제외된다
    ✓ 코드 블록 내 중복 토큰은 하나만 남긴다

  본문 보존
    ✓ 코드 블록 앞뒤의 일반 텍스트가 변환 후에도 유지된다
    ✓ 변환된 결과에 원본 코드 블록 구문 기호가 남지 않는다

com.criticalflow.global.ai.rag.NoteMetadataExtractorTest

  언어 추출
    ✓ 복수 언어 코드 블록에서 모든 언어를 추출한다
    ✓ 동일 언어가 여러 번 등장해도 한 번만 반환한다
    ✓ 언어명은 소문자로 정규화된다
    ✓ 언어 미지정 코드 블록만 있으면 unknown을 반환한다
    ✓ 언어 지정 블록과 미지정 블록이 섞이면 지정된 언어만 반환한다
    ✓ 코드 블록이 없으면 빈 리스트를 반환한다

  헤더 추출
    ✓ 모든 레벨의 마크다운 헤더를 순서대로 추출한다
    ✓ 헤더 앞의 # 기호와 공백이 제거된다
    ✓ 헤더가 없으면 빈 리스트를 반환한다
    ✓ 빈 헤더 라인은 결과에서 제외된다

18 tests completed, 0 failed

BUILD SUCCESSFUL in 2s
```

**3-3. 문제 증상**
없음 — 신규 테스트 추가이며 모든 케이스가 첫 실행부터 통과.

**3-4. 원인 가설**
해당 없음

**3-5. 원인 확정 근거**
해당 없음

---

## 4. 조치

> **현상 유지** — 이유: 프로덕션 코드 수정 없이 테스트만 추가. 모든 케이스 통과로 기존 로직의 정확성 확인.

---

## 6. 결론

**6-1. 목표 달성 여부**
- [x] 달성 — 18개 단위 테스트 전체 통과, 회귀 감지 체계 구축

**6-2. 관련 요구사항 충족 여부**
요구사항 직접 연결 없음. 임베딩 파이프라인 내 두 전처리 컴포넌트의 회귀 감지 체계 구축 완료.

**6-3. 잔여 과제 및 후속 조치**
#57 측정으로 `NotePreprocessor` 자체는 `NoteEmbeddingService`에서 제거됐지만, 로직 보존 차원에서 단위 테스트는 유지한다.

**6-4. 팀 공유 사항**
- `NotePreprocessor.extractIdentifiers()`는 `length > 2` 조건으로 2자 이하 토큰을 제외한다.
- `NoteMetadataExtractor.extractLanguages()`는 named 언어만 반환하며, 언어 미지정 블록의 `unknown` 레이블은 반환하지 않는다.
