package com.criticalflow.global.ai.rag;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class NoteMetadataExtractor {

    private static final Pattern CODE_LANG = Pattern.compile("```(\\w+)");
    private static final Pattern CODE_BLOCK_NO_LANG = Pattern.compile("```(?!\\w)");

    public List<String> extractLanguages(String markdown) {
        List<String> languages = CODE_LANG.matcher(markdown).results()
                .map(r -> r.group(1).toLowerCase())
                .distinct()
                .collect(Collectors.toList());

        // 언어 미지정 코드 블록이 있으면 "unknown" 추가
        if (languages.isEmpty()) {
            Matcher noLangMatcher = CODE_BLOCK_NO_LANG.matcher(markdown);
            if (noLangMatcher.find()) {
                languages.add("unknown");
            }
        }

        return languages;
    }

    public List<String> extractHeaders(String markdown) {
        return Arrays.stream(markdown.split("\n"))
                .filter(line -> line.startsWith("#"))
                .map(line -> line.replaceAll("^#+\\s*", "").trim())
                .filter(h -> !h.isEmpty())
                .toList();
    }
}
