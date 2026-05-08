package com.criticalflow.global.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagRetrievalService {

    private final VectorStore vectorStore;

    @Value("${rag.similarity-threshold:0.75}")
    private double similarityThreshold;

    @Value("${rag.max-results:4}")
    private int maxResults;

    @Value("${rag.bm25-max-results:10}")
    private int bm25MaxResults;

    /**
     * Dense + Sparse 하이브리드 검색 후 RRF로 병합한다.
     *
     * Dense  — 의미 기반 벡터 유사도 검색 (threshold 0.75)
     * Sparse — 키워드 포함 여부 기반 검색 (threshold 0.0 + 키워드 필터)
     * RRF    — 두 순위 리스트를 1/(k+rank) 점수로 병합
     */
    public RagContext retrieve(String queryText, Long userId, Long excludeNoteId) {
        List<Document> denseResults  = denseSearch(queryText, userId, excludeNoteId);
        List<Document> sparseResults = sparseSearch(queryText, userId, excludeNoteId);

        List<Document> merged = mergeWithRRF(denseResults, sparseResults, maxResults);

        // [#59 임시 로그 — 2차 필터 노이즈 감소 효과 측정 후 제거]
        long beforeFilter = merged.size();
        List<RagContext.RetrievedChunk> chunks = merged.stream()
                .filter(doc -> isTopicRelevant(doc, queryText))
                .map(doc -> RagContext.RetrievedChunk.builder()
                        .title(getMeta(doc, "title"))
                        .sessionId(getMeta(doc, "session_id"))
                        .content(doc.getText())
                        .score(extractScore(doc))
                        .build())
                .toList();
        if (beforeFilter > 0) {
            long afterFilter = chunks.size();
            String rate = String.format("%.1f", (beforeFilter - afterFilter) * 100.0 / beforeFilter);
            log.info("[RAG 필터] 쿼리='{}' | RRF 후: {}건 → 2차 필터 후: {}건 | 제거율: {}%",
                    queryText, beforeFilter, afterFilter, rate);
        }
        // [#59 임시 로그 끝]

        return RagContext.builder().chunks(chunks).build();
    }

    // ── Dense 검색 ────────────────────────────────────────────────────────────

    private List<Document> denseSearch(String queryText, Long userId, Long excludeNoteId) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(queryText)
                .topK(maxResults + 2)
                .similarityThreshold(similarityThreshold)
                .filterExpression("user_id == '" + userId + "' && note_id != '" + excludeNoteId + "'")
                .build());
    }

    // ── Sparse 검색 (키워드 기반) ─────────────────────────────────────────────

    private List<Document> sparseSearch(String queryText, Long userId, Long excludeNoteId) {
        List<String> keywords = extractKeyTerms(queryText);
        if (keywords.isEmpty()) return List.of();

        // threshold 0.0으로 넓게 후보를 가져온 뒤 키워드 포함 여부로 필터
        List<Document> candidates = vectorStore.similaritySearch(SearchRequest.builder()
                .query(queryText)
                .topK(bm25MaxResults)
                .similarityThreshold(0.0)
                .filterExpression("user_id == '" + userId + "' && note_id != '" + excludeNoteId + "'")
                .build());

        return candidates.stream()
                .filter(doc -> {
                    String content = doc.getText().toLowerCase();
                    return keywords.stream().anyMatch(content::contains);
                })
                .collect(Collectors.toList());
    }

    private List<String> extractKeyTerms(String queryText) {
        boolean hasKorean = queryText.chars().anyMatch(c -> c >= 0xAC00 && c <= 0xD7A3);
        int minLength = hasKorean ? 1 : 4;

        return Arrays.stream(queryText.toLowerCase().split("\\s+"))
                .filter(k -> k.length() >= minLength)
                .distinct()
                .toList();
    }

    // ── RRF 병합 ─────────────────────────────────────────────────────────────

    private List<Document> mergeWithRRF(List<Document> dense, List<Document> sparse, int limit) {
        Map<String, Double> scores = new HashMap<>();
        Map<String, Document> docMap = new HashMap<>();
        int k = 60; // RRF 표준 상수

        for (int i = 0; i < dense.size(); i++) {
            String id = dense.get(i).getId();
            scores.merge(id, 1.0 / (k + i + 1), Double::sum);
            docMap.put(id, dense.get(i));
        }
        for (int i = 0; i < sparse.size(); i++) {
            String id = sparse.get(i).getId();
            scores.merge(id, 1.0 / (k + i + 1), Double::sum);
            docMap.putIfAbsent(id, sparse.get(i));
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> docMap.get(e.getKey()))
                .filter(Objects::nonNull)
                .toList();
    }

    // ── 2차 필터 ──────────────────────────────────────────────────────────────

    private boolean isTopicRelevant(Document doc, String queryText) {
        String content = doc.getText().toLowerCase();

        boolean hasKorean = queryText.chars().anyMatch(c -> c >= 0xAC00 && c <= 0xD7A3);
        // 한국어 2자 이상, 영어 4자 이상만 의미 있는 키워드로 인정
        // queryText가 노트 전체 내용일 때 단순 조사/어미가 키워드로 잡히는 것을 방지
        int minLength = hasKorean ? 2 : 4;

        List<String> keywords = Arrays.stream(queryText.toLowerCase().split("\\s+"))
                .filter(k -> k.length() >= minLength)
                .distinct()
                .limit(20) // 긴 노트 쿼리 대응: 상위 20개 키워드만 사용
                .collect(Collectors.toList());

        if (keywords.isEmpty()) return true;

        long matched = keywords.stream().filter(content::contains).count();

        // 임계값 0.1: 노트 전체를 쿼리로 쓸 때 20% 기준은 과도하게 엄격함
        return (double) matched / keywords.size() >= 0.1;
    }

    private String getMeta(Document doc, String key) {
        Object val = doc.getMetadata().get(key);
        return val != null ? val.toString() : "";
    }

    private double extractScore(Document doc) {
        Object raw = doc.getMetadata().get("distance");
        if (raw instanceof Number n) return n.doubleValue();
        return 0.0;
    }
}
