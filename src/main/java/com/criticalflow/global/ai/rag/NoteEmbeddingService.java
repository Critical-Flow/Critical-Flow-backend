package com.criticalflow.global.ai.rag;

import com.criticalflow.domain.note.entity.StudyNote;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class
NoteEmbeddingService {

    private final VectorStore vectorStore;

    /**
     * 노트를 Chroma에 임베딩한다.
     * 동일 note_id로 재저장 시 기존 벡터를 먼저 삭제하여 버전 일관성을 유지한다.
     */
    public void embed(StudyNote note) {
        String documentId = toDocumentId(note.getNoteId());
        vectorStore.delete(List.of(documentId));

        Document document = new Document(
                documentId,
                note.getContent(),
                Map.of(
                        "note_id",    note.getNoteId().toString(),
                        "session_id", note.getSessionId().toString(),
                        "user_id",    note.getUserId().toString(),
                        "title",      note.getTitle(),
                        "created_at", note.getCreatedAt().toString()
                )
        );

        vectorStore.add(List.of(document));
    }

    /**
     * 노트 삭제 시 벡터 스토어에서도 제거한다.
     */
    public void delete(Long noteId) {
        vectorStore.delete(List.of(toDocumentId(noteId)));
    }

    private String toDocumentId(Long noteId) {
        return "note-" + noteId;
    }
}