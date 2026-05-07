package com.criticalflow.global.ai.router;

import com.criticalflow.domain.ai.entity.QuestionType;
import com.criticalflow.domain.note.entity.StudyNote;
import com.criticalflow.global.ai.rag.RagContext;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class QuestionTypeRouter {

    private final ChatModel chatModel;

    @Value("classpath:prompts/type-router.st")
    private Resource routerPromptResource;

    private String routerPromptTemplate;

    @PostConstruct
    private void loadPrompt() throws IOException {
        routerPromptTemplate = routerPromptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    public QuestionType route(StudyNote note, RagContext ragContext) {
        // 규칙 기반 필터: 코드 블록 존재 시 TYPE_E 즉시 반환
        if (note.getContent().contains("```")) {
            return QuestionType.TYPE_E;
        }

        String prompt = routerPromptTemplate
                .replace("{note_content}", note.getContent())
                .replace("{rag_available}", String.valueOf(!ragContext.isEmpty()));

        String raw = chatModel.call(new Prompt(new UserMessage(prompt)))
                .getResult()
                .getOutput()
                .getText()
                .trim()
                .toUpperCase();

        return parseType(raw, ragContext);
    }

    private QuestionType parseType(String raw, RagContext ragContext) {
        for (QuestionType type : QuestionType.values()) {
            if (raw.contains(type.name())) {
                // TYPE_C는 RAG 결과가 없으면 TYPE_A로 폴백
                if (type == QuestionType.TYPE_C && ragContext.isEmpty()) {
                    return QuestionType.TYPE_A;
                }
                return type;
            }
        }
        return QuestionType.TYPE_A;
    }
}
