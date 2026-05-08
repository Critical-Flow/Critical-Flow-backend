package com.criticalflow.global.ai.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoteMetadataExtractorTest {

    private final NoteMetadataExtractor extractor = new NoteMetadataExtractor();

    @Nested
    @DisplayName("언어 추출")
    class LanguageExtractionTest {

        @Test
        @DisplayName("복수 언어 코드 블록에서 모든 언어를 추출한다")
        void extractsAllLanguagesFromMultipleBlocks() {
            String markdown = "```python\ncode\n```\n```java\ncode\n```";

            List<String> languages = extractor.extractLanguages(markdown);

            assertThat(languages).containsExactlyInAnyOrder("python", "java");
        }

        @Test
        @DisplayName("동일 언어가 여러 번 등장해도 한 번만 반환한다")
        void deduplicatesSameLanguage() {
            String markdown = "```python\nfirst\n```\n```python\nsecond\n```";

            List<String> languages = extractor.extractLanguages(markdown);

            assertThat(languages).containsExactly("python");
        }

        @Test
        @DisplayName("언어명은 소문자로 정규화된다")
        void normalizesLanguageToLowerCase() {
            String markdown = "```Java\ncode\n```";

            List<String> languages = extractor.extractLanguages(markdown);

            assertThat(languages).containsExactly("java");
        }

        @Test
        @DisplayName("언어 미지정 코드 블록만 있으면 unknown을 반환한다")
        void returnsUnknownForUnspecifiedLanguageBlock() {
            String markdown = "```\nsome code\n```";

            List<String> languages = extractor.extractLanguages(markdown);

            assertThat(languages).containsExactly("unknown");
        }

        @Test
        @DisplayName("언어 지정 블록과 미지정 블록이 섞이면 지정된 언어만 반환한다")
        void returnsOnlyNamedLanguagesWhenMixed() {
            String markdown = "```python\ncode\n```\n```\ncode\n```";

            List<String> languages = extractor.extractLanguages(markdown);

            assertThat(languages).containsExactly("python");
            assertThat(languages).doesNotContain("unknown");
        }

        @Test
        @DisplayName("코드 블록이 없으면 빈 리스트를 반환한다")
        void returnsEmptyListWhenNoCodeBlock() {
            String markdown = "코드 블록 없는 일반 텍스트입니다";

            assertThat(extractor.extractLanguages(markdown)).isEmpty();
        }
    }

    @Nested
    @DisplayName("헤더 추출")
    class HeaderExtractionTest {

        @Test
        @DisplayName("모든 레벨의 마크다운 헤더를 순서대로 추출한다")
        void extractsAllHeaderLevelsInOrder() {
            String markdown = "# 1레벨\n내용\n## 2레벨\n내용\n### 3레벨";

            List<String> headers = extractor.extractHeaders(markdown);

            assertThat(headers).containsExactly("1레벨", "2레벨", "3레벨");
        }

        @Test
        @DisplayName("헤더 앞의 # 기호와 공백이 제거된다")
        void stripsHashAndWhitespaceFromHeaders() {
            String markdown = "## 스택과 큐";

            List<String> headers = extractor.extractHeaders(markdown);

            assertThat(headers).containsExactly("스택과 큐");
        }

        @Test
        @DisplayName("헤더가 없으면 빈 리스트를 반환한다")
        void returnsEmptyListWhenNoHeaders() {
            String markdown = "헤더 없는 일반 텍스트입니다\n두 번째 줄";

            assertThat(extractor.extractHeaders(markdown)).isEmpty();
        }

        @Test
        @DisplayName("빈 헤더 라인은 결과에서 제외된다")
        void excludesEmptyHeaderLines() {
            String markdown = "# \n## 유효한 헤더";

            List<String> headers = extractor.extractHeaders(markdown);

            assertThat(headers).containsExactly("유효한 헤더");
        }
    }
}
