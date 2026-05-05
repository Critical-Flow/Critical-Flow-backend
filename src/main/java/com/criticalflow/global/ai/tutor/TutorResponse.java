package com.criticalflow.global.ai.tutor;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TutorResponse {

    private final String content;

    /** LAW 3: question_count >= 5 이면 true — 클라이언트가 UI 잠금 등을 처리할 수 있도록 노출 */
    private final boolean summaryMode;

    private final int questionCount;
}
