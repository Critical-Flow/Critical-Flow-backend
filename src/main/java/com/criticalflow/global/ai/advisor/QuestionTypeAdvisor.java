package com.criticalflow.global.ai.advisor;

import com.criticalflow.domain.ai.entity.QuestionType;
import com.criticalflow.domain.note.entity.StudyNote;
import com.criticalflow.global.ai.rag.RagContext;
import com.criticalflow.global.ai.router.QuestionTypeRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestionTypeAdvisor implements CallAroundAdvisor {

    private final QuestionTypeRouter router;
    private final QuestionTypePromptProvider promptProvider;

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest req, CallAroundAdvisorChain chain) {
        StudyNote note = (StudyNote) req.adviseContext().get("note");
        RagContext ragContext = (RagContext) req.adviseContext().get("ragContext");

        QuestionType type = router.route(note, ragContext);

        AdvisedRequest modified = req.mutate()
                .systemText(promptProvider.inject(req.systemText(), type))
                .build();

        return chain.nextAroundCall(modified);
    }

    @Override
    public String getName() {
        return "QuestionTypeAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
