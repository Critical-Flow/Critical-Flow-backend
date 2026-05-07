package com.criticalflow.global.ai.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RagRetrievalService {

    private final VectorStore vectorStore;

    @Value("${rag.similarity-threshold:0.75}")
    private double similarityThreshold;

    @Value("${rag.max-results:4}")
    private int maxResults;

    /**
     * 현재 노트 내용을 쿼리로 Chroma를 검색한다.
     *
     * 1차 필터 — Chroma의 similarity threshold (0.75)
     * 2차 필터 — 키워드 오버랩 기반 topic mismatch 제거 (LAW 4: RAG 노이즈 차단)
     */
    public RagContext retrieve(String queryText, Long userId) {
        SearchRequest request = SearchRequest.builder()
                .query(queryText)
                .topK(maxResults + 2)                      // 여유 있게 가져와서 2차 필터 후 자름
                .similarityThreshold(similarityThreshold)
                .filterExpression("user_id == '" + userId + "'")
                .build();

        List<Document> candidates = vectorStore.similaritySearch(request);

        List<RagContext.RetrievedChunk> chunks = candidates.stream()
                .filter(doc -> isTopicRelevant(doc, queryText))
                .limit(maxResults)
                .map(doc -> RagContext.RetrievedChunk.builder()
                        .title(getMeta(doc, "title"))
                        .sessionId(getMeta(doc, "session_id"))
                        .content(doc.getText())
                        .score(extractScore(doc))
                        .build())
                .toList();

        return RagContext.builder().chunks(chunks).build();
    }

    /**
     * 2차 topic mismatch 필터.
     * Chroma threshold를 간신히 넘긴 노이즈 청크를 제거한다.
     * 한국어 포함 시 최소 길이 1자, 영어는 3자 기준 — 의미 있는 키워드의 최소 20%가 겹쳐야 통과.
     */
    private boolean isTopicRelevant(Document doc, String queryText) {
        String[] keywords = queryText.toLowerCase().split("\\s+");
        String content = doc.getText().toLowerCase();

        boolean hasKorean = queryText.chars().anyMatch(c -> c >= 0xAC00 && c <= 0xD7A3);
        int minLength = hasKorean ? 1 : 3;

        long significant = Arrays.stream(keywords).filter(k -> k.length() >= minLength).count();
        if (significant == 0) return true;

        long matched = Arrays.stream(keywords)
                .filter(k -> k.length() >= minLength)
                .filter(content::contains)
                .count();

        return (double) matched / significant >= 0.2;
    }

    private String getMeta(Document doc, String key) {
        Object val = doc.getMetadata().get(key);
        return val != null ? val.toString() : "";
    }

    private double extractScore(Document doc) {
        // Spring AI는 score를 Document 메타데이터의 "distance" 키에 저장한다
        Object raw = doc.getMetadata().get("distance");
        if (raw instanceof Number n) return n.doubleValue();
        return 0.0;
    }
}