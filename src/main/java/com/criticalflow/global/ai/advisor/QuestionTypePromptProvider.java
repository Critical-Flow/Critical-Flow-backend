package com.criticalflow.global.ai.advisor;

import com.criticalflow.domain.conversation.entity.QuestionType;
import org.springframework.stereotype.Component;

@Component
public class QuestionTypePromptProvider {

    public String getPromptFor(QuestionType type) {
        return switch (type) {
            case TYPE_A -> """
                    TYPE A — 개념 정의 확인 및 적용 탐구
                      목표: 학습자가 새로운 용어나 개념을 자신의 말로 설명할 수 있는지 확인하고,
                           이론만 있는 경우 실제 상황에 적용할 수 있는지도 함께 탐구한다.
                      질문 템플릿 (하나 선택):
                        - "[용어]에 대해 찾아보지 않고 본인의 말로 설명해줄 수 있어?"
                        - "[개념]이 실제로 어떤 문제를 해결하기 위해 등장했나요?"
                        - "지금까지 개발하거나 공부하면서 [개념]이 필요했던 순간을 떠올릴 수 있나요?"
                    """;
            case TYPE_B -> """
                    TYPE B — 설계 의도 탐침
                      목표: 학습자가 특정 방식을 선택한 것이 의도적인 결정인지 확인한다.
                      질문 템플릿: "[방식] 대신 [대안]을 선택하지 않은 이유가 뭐야? 어떤 트레이드오프를 생각했어?
                    """;
            case TYPE_C -> """
                    TYPE C — 과거 학습 연계 (Spaced Recall)
                      목표: 현재 학습 내용과 이전에 공부한 개념을 연결하여 장기 기억을 강화한다.
                      질문 템플릿: "예전에 [과거 주제]를 공부했는데, 지금 배우는 내용과 어떻게 연결되는 것 같아?
                    """;
            case TYPE_D -> """
                    TYPE D — 심층 사고 탐구
                      목표: 표면적 이해를 넘어 엣지케이스, 실패 케이스, 경계 조건을 탐구한다.
                      질문 템플릿: "[전제 조건]이 성립하지 않으면 어떻게 돼? 어떤 문제가 생기는지 설명해줄 수 있어?
                    """;
            case TYPE_E -> """
                    TYPE E — 코드 동작 탐침
                      목표: 학습자가 코드가 실제로 어떻게 동작하는지 이해하고 있는지 확인한다.
                      질문 템플릿 (하나 선택):
                        - "이 코드의 시간복잡도는 어떻게 되나요? 왜 그렇게 생각하나요?"
                        - "이 코드에서 [특정 라인]이 없다면 어떤 일이 생길까요?"
                        - "이 함수에 [엣지케이스 입력]을 넣으면 어떻게 동작할까요?"
                      제약: 수정된 코드나 정답 구현을 절대 제공하지 않는다 (LAW 1).
                    """;
        };
    }

    public String inject(String systemText, QuestionType type) {
        return systemText.replace("{selected_question_type}", getPromptFor(type));
    }
}
