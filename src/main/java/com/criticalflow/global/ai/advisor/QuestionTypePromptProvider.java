package com.criticalflow.global.ai.advisor;

import com.criticalflow.domain.conversation.entity.QuestionType;
import org.springframework.stereotype.Component;

@Component
public class QuestionTypePromptProvider {

    public String getPromptFor(QuestionType type) {
        return switch (type) {
            case TYPE_A -> """
                    TYPE A — Concept Verification
                      Goal: check whether the learner can define the new term in their own words.
                      Template: "You mentioned [TERM]. Can you explain what that means without looking it up?"
                    """;
            case TYPE_B -> """
                    TYPE B — Design Intent Probe
                      Goal: surface whether the design choice was deliberate or accidental.
                      Template: "Why did you choose [APPROACH] instead of [ALTERNATIVE]? What tradeoff were you making?"
                    """;
            case TYPE_C -> """
                    TYPE C — Spaced Recall Quiz
                      Goal: reconnect current learning with prior knowledge from past notes.
                      Template: "You studied [PAST TOPIC] before. How does that connect to what you're learning now?"
                    """;
            case TYPE_D -> """
                    TYPE D — Deep Thinking Probe
                      Goal: push beyond surface understanding into edge cases or failure modes.
                      Template: "What breaks if [ASSUMPTION] is false? Walk me through the failure mode."
                    """;
            case TYPE_E -> """
                    TYPE E — Code Behavior Probe
                      Goal: check whether the learner understands what the code actually does.
                      Template options:
                        - "이 코드의 시간복잡도는 어떻게 되나요? 왜 그렇게 생각하나요?"
                        - "이 코드에서 [특정 라인]이 없다면 어떤 일이 생길까요?"
                        - "이 함수에 [엣지케이스 입력]을 넣으면 어떻게 동작할까요?"
                      Constraint: NEVER provide corrected code (LAW 1).
                    """;
            case TYPE_F -> """
                    TYPE F — Contextual Application Probe
                      Goal: check whether the learner can apply the concept to a real situation.
                      Template options:
                        - "[개념]이 실제로 어떤 문제를 해결하기 위해 등장했나요?"
                        - "[개념 A]와 [개념 B]는 어떤 상황에서 각각 선택하나요?"
                    """;
        };
    }

    public String inject(String systemText, QuestionType type) {
        return systemText.replace("{selected_question_type}", getPromptFor(type));
    }
}
