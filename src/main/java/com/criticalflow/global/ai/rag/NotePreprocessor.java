package com.criticalflow.global.ai.rag;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class NotePreprocessor {

    private static final Pattern CODE_BLOCK = Pattern.compile("```(\\w+)?\\n([\\s\\S]*?)```");

    /**
     * 임베딩 전 마크다운 노트를 전처리한다.
     * 코드 블록을 의미 있는 식별자/주석 텍스트로 변환해 한국어 설명과 코드가
     * 혼합될 때 발생하는 벡터 희석 문제를 완화한다.
     */
    public String preprocessForEmbedding(String markdown) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = CODE_BLOCK.matcher(markdown);
        int last = 0;

        while (matcher.find()) {
            result.append(markdown, last, matcher.start());

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
        return Arrays.stream(code.split("[^a-zA-Z가-힣_]+"))
                .filter(token -> token.length() > 2)
                .distinct()
                .collect(Collectors.joining(" "));
    }
}
