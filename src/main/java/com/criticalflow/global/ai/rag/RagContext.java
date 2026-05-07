package com.criticalflow.global.ai.rag;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class RagContext {

    private final List<RetrievedChunk> chunks;

    public boolean isEmpty() {
        return chunks == null || chunks.isEmpty();
    }

    /**
     * System Prompt의 {rag_context} 변수에 주입될 문자열로 직렬화.
     * 각 청크는 출처(제목, 세션, 유사도 점수)와 함께 구분자로 분리된다.
     */
    public String format() {
        if (isEmpty()) {
            return "(No relevant past notes found.)";
        }
        return "[참고용 과거 노트 — 현재 노트 학습 보조 목적으로만 활용]\n\n"
                + chunks.stream()
                .map(c -> String.format(
                        "--- [Note: \"%s\" | session_id: %s | similarity: %.2f] ---\n%s",
                        c.getTitle(), c.getSessionId(), c.getScore(), c.getContent()))
                .collect(Collectors.joining("\n\n"));
    }

    @Getter
    @Builder
    public static class RetrievedChunk {
        private final String title;
        private final String sessionId;
        private final String content;
        private final double score;
    }
}