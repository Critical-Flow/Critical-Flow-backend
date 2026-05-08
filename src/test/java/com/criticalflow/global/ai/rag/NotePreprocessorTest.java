package com.criticalflow.global.ai.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotePreprocessorTest {

    private final NotePreprocessor preprocessor = new NotePreprocessor();

    @Nested
    @DisplayName("코드 블록 변환")
    class CodeBlockTransformTest {

        @Test
        @DisplayName("코드 블록이 [언어: 식별자] 형태로 변환된다")
        void convertsCodeBlockToIdentifierText() {
            String input = "개요\n```python\ndef fibonacci(n):\n    return n\n```\n설명";

            String result = preprocessor.preprocessForEmbedding(input);

            assertThat(result).contains("[python:");
            assertThat(result).contains("fibonacci");
            assertThat(result).contains("return");
        }

        @Test
        @DisplayName("코드 블록이 없으면 원문을 그대로 반환한다")
        void returnsOriginalWhenNoCodeBlock() {
            String input = "스택은 LIFO 구조의 자료구조다";

            assertThat(preprocessor.preprocessForEmbedding(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("언어 미지정 코드 블록은 [code: ...] 레이블을 사용한다")
        void usesCodeLabelForUnspecifiedLanguage() {
            String input = "```\nsome code here\n```";

            assertThat(preprocessor.preprocessForEmbedding(input)).contains("[code:");
        }

        @Test
        @DisplayName("복수 코드 블록이 있으면 모두 변환된다")
        void convertsAllCodeBlocks() {
            String input = "```python\ndef foo():\n    pass\n```\n중간 설명\n```java\npublic void bar() {}\n```";

            String result = preprocessor.preprocessForEmbedding(input);

            assertThat(result).contains("[python:");
            assertThat(result).contains("[java:");
        }
    }

    @Nested
    @DisplayName("식별자 필터링")
    class IdentifierFilterTest {

        @Test
        @DisplayName("2자 이하 토큰은 식별자에서 제외된다")
        void excludesTokensOfLengthTwoOrLess() {
            // "ab"(2자), "cd"(2자) 는 제외, "abc"(3자) 는 포함
            String input = "```python\nab cd abc\n```";

            String result = preprocessor.preprocessForEmbedding(input);

            assertThat(result).doesNotContain(" ab ").doesNotContain(" cd ");
            assertThat(result).contains("abc");
        }

        @Test
        @DisplayName("코드 블록 내 중복 토큰은 하나만 남긴다")
        void deduplicatesIdentifiers() {
            String input = "```python\ndef foo():\n    return foo\n```";

            String result = preprocessor.preprocessForEmbedding(input);

            // "foo"가 두 번 등장하지만 결과에는 한 번만
            long fooCount = result.chars()
                    .mapToObj(c -> String.valueOf((char) c))
                    .collect(java.util.stream.Collectors.joining())
                    .split("foo").length - 1;
            assertThat(fooCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("본문 보존")
    class PlainTextPreservationTest {

        @Test
        @DisplayName("코드 블록 앞뒤의 일반 텍스트가 변환 후에도 유지된다")
        void preservesPlainTextAroundCodeBlock() {
            String input = "서론 내용\n```java\npublic void method() {}\n```\n결론 내용";

            String result = preprocessor.preprocessForEmbedding(input);

            assertThat(result).contains("서론 내용");
            assertThat(result).contains("결론 내용");
        }

        @Test
        @DisplayName("변환된 결과에 원본 코드 블록 구문 기호가 남지 않는다")
        void removesCodeBlockDelimiters() {
            String input = "```python\ndef foo():\n    pass\n```";

            String result = preprocessor.preprocessForEmbedding(input);

            assertThat(result).doesNotContain("```");
        }
    }
}
