package com.criticalflow.global.ai.tutor;

import com.criticalflow.domain.ai.entity.AiConversation;
import com.criticalflow.domain.ai.entity.AiMessage;
import com.criticalflow.domain.ai.entity.AiMessage.MessageRole;
import com.criticalflow.domain.ai.repository.AiConversationRepository;
import com.criticalflow.domain.ai.repository.AiMessageRepository;
import com.criticalflow.domain.note.entity.StudyNote;
import com.criticalflow.domain.note.repository.StudyNoteRepository;
import com.criticalflow.global.ai.rag.FocusEventFormatter;
import com.criticalflow.global.ai.rag.RagContext;
import com.criticalflow.global.ai.rag.RagRetrievalService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiTutorService {

    private static final int MAX_QUESTIONS = 5;

    private static final Set<String> FILLER_WORDS = Set.of(
            "아", "어", "음", "몰라", "모르겠어", "모름", ".", "..", "...", "idk", "?"
    );

    private final ChatModel chatModel;
    private final RagRetrievalService ragRetrievalService;
    private final FocusEventFormatter focusEventFormatter;
    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final StudyNoteRepository noteRepository;

    @Value("classpath:prompts/tutor-system.st")
    private Resource systemPromptResource;

    private String systemPromptTemplate;

    @PostConstruct
    private void loadPromptTemplate() throws IOException {
        systemPromptTemplate = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    @Transactional
    public TutorResponse generateFirstQuestion(Long conversationId) {
        AiConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        StudyNote note = noteRepository.findById(conversation.getNoteId())
                .orElseThrow(() -> new IllegalStateException("Note not found for conversation: " + conversationId));

        RagContext ragContext = ragRetrievalService.retrieve(note.getContent(), note.getUserId());
        String focusEvents = focusEventFormatter.format(note.getSessionId());

        String resolvedPrompt = resolvePrompt(
                note.getContent(),
                ragContext.format(),
                focusEvents,
                conversation.getType().name(),
                0
        );

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(resolvedPrompt));
        ChatResponse chatResponse = chatModel.call(new Prompt(messages));
        String aiContent = chatResponse.getResult().getOutput().getText();

        persistMessage(conversationId, MessageRole.AI, aiContent, 1);

        return TutorResponse.builder()
                .content(aiContent)
                .summaryMode(false)
                .questionCount(0)
                .build();
    }

    @Transactional
    public TutorResponse respond(Long conversationId, String userMessage) {
        AiConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        StudyNote note = noteRepository.findById(conversation.getNoteId())
                .orElseThrow(() -> new IllegalStateException("Note not found for conversation: " + conversationId));

        List<AiMessage> history = messageRepository.findByConversationIdOrderBySequenceAsc(conversationId);
        long questionCount = countAiMessages(history);

        // LAW 5: 무의미한 입력은 LLM 호출 없이 재유도 응답 반환
        if (isMeaninglessInput(userMessage)) {
            return reanchor(conversationId, userMessage, history, questionCount);
        }

        persistMessage(conversationId, MessageRole.USER, userMessage, history.size() + 1);

        RagContext ragContext = ragRetrievalService.retrieve(note.getContent(), note.getUserId());
        String focusEvents = focusEventFormatter.format(note.getSessionId());

        String resolvedPrompt = resolvePrompt(
                note.getContent(),
                ragContext.format(),
                focusEvents,
                conversation.getType().name(),
                questionCount
        );

        List<Message> messages = buildMessages(resolvedPrompt, history, userMessage);
        ChatResponse chatResponse = chatModel.call(new Prompt(messages));
        String aiContent = chatResponse.getResult().getOutput().getText();

        persistMessage(conversationId, MessageRole.AI, aiContent, history.size() + 2);

        return TutorResponse.builder()
                .content(aiContent)
                .summaryMode(questionCount + 1 >= MAX_QUESTIONS)
                .questionCount((int) questionCount + 1)
                .build();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private boolean isMeaninglessInput(String message) {
        if (message == null) return true;
        String trimmed = message.trim();
        if (trimmed.length() < 5) return true;
        return FILLER_WORDS.contains(trimmed.toLowerCase());
    }

    /**
     * LAW 5 발동: LLM 없이 마지막 AI 질문을 단순화해 재제시한다.
     */
    private TutorResponse reanchor(Long conversationId, String userMessage,
                                   List<AiMessage> history, long questionCount) {
        String lastAiQuestion = history.stream()
                .filter(m -> m.getRole() == MessageRole.AI)
                .reduce((first, second) -> second)
                .map(AiMessage::getContent)
                .orElse("어떤 부분이 막히는지 조금 더 구체적으로 말해줄 수 있어?");

        String reanchorContent = "조금 막히는 것 같네. 다시 한 번 — " + lastAiQuestion;

        persistMessage(conversationId, MessageRole.USER, userMessage, history.size() + 1);
        persistMessage(conversationId, MessageRole.AI, reanchorContent, history.size() + 2);

        return TutorResponse.builder()
                .content(reanchorContent)
                .summaryMode(false)
                .questionCount((int) questionCount)
                .build();
    }

    private String resolvePrompt(String currentNote, String ragContext, String focusEvents,
                                  String conversationType, long questionCount) {
        return systemPromptTemplate
                .replace("{current_note}", currentNote)
                .replace("{rag_context}", ragContext)
                .replace("{focus_events}", focusEvents)
                .replace("{conversation_type}", conversationType)
                .replace("{question_count}", questionCount + " / " + MAX_QUESTIONS);
    }

    private List<Message> buildMessages(String systemPrompt, List<AiMessage> history, String userMessage) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        for (AiMessage msg : history) {
            messages.add(msg.getRole() == MessageRole.USER
                    ? new UserMessage(msg.getContent())
                    : new AssistantMessage(msg.getContent()));
        }
        messages.add(new UserMessage(userMessage));
        return messages;
    }

    private long countAiMessages(List<AiMessage> history) {
        return history.stream().filter(m -> m.getRole() == MessageRole.AI).count();
    }

    private void persistMessage(Long conversationId, MessageRole role, String content, int sequence) {
        messageRepository.save(AiMessage.of(conversationId, role, content, sequence));
    }
}
