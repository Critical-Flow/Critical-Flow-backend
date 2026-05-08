package com.criticalflow.global.ai.advisor;

import com.criticalflow.domain.conversation.entity.QuestionType;
import com.criticalflow.domain.note.entity.StudyNote;
import com.criticalflow.global.ai.rag.RagContext;
import com.criticalflow.global.ai.router.QuestionTypeRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionTypeAdvisor implements CallAdvisor {

    private final QuestionTypeRouter router;
    private final QuestionTypePromptProvider promptProvider;

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest req, CallAdvisorChain chain) {
        StudyNote note = (StudyNote) req.context().get("note");
        RagContext ragContext = (RagContext) req.context().get("ragContext");

        // AiTutorService에서 사전에 결정된 TYPE이 있으면 재사용, 없으면 직접 라우팅
        QuestionType preSelected = (QuestionType) req.context().get("preSelectedType");
        QuestionType type = (preSelected != null) ? preSelected : router.route(note, ragContext);

        log.info("[QuestionTypeAdvisor] noteId={} → TYPE={}", note.getNoteId(), type);

        Prompt originalPrompt = req.prompt();
        List<Message> modifiedMessages = originalPrompt.getInstructions().stream()
                .map(msg -> {
                    if (msg instanceof SystemMessage) {
                        return (Message) new SystemMessage(
                                promptProvider.inject(msg.getText(), type));
                    }
                    return msg;
                })
                .toList();

        Prompt newPrompt = new Prompt(modifiedMessages, originalPrompt.getOptions());
        ChatClientRequest modifiedReq = req.mutate().prompt(newPrompt).build();

        return chain.nextCall(modifiedReq);
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
