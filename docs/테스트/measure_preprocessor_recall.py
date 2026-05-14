"""
#57 NotePreprocessor 임베딩 품질 비교 — Recall@4 측정 스크립트

사용법:
  python measure_preprocessor_recall.py

사전 준비:
  pip install chromadb
  두 조건(without/with preprocessor)으로 노트가 이미 임베딩되어 있어야 함
"""

import chromadb
import json

CHROMA_HOST = "localhost"
CHROMA_PORT = 8000

WITHOUT_COLLECTION = "test-without-preprocessor"
WITH_COLLECTION    = "test-with-preprocessor"

# 10개 쿼리 + Ground Truth (노트 ID는 서버 응답에서 확인 후 직접 입력)
# note_id 는 POST /api/notes 응답의 noteId 값으로 채워야 함
QUERIES = [
    {
        "query": "피보나치 수열 재귀 메모이제이션 구현",
        "relevant_note_ids": ["note-{NOTE_1_ID}"],  # Note 1 — fibonacci
    },
    {
        "query": "이진 탐색 left right mid 구현",
        "relevant_note_ids": ["note-{NOTE_2_ID}"],  # Note 2 — BinarySearch
    },
    {
        "query": "병합 정렬 merge 함수 분할 정복",
        "relevant_note_ids": ["note-{NOTE_3_ID}"],  # Note 3 — mergeSort
    },
    {
        "query": "HashMap put get getOrDefault computeIfAbsent",
        "relevant_note_ids": ["note-{NOTE_4_ID}"],  # Note 4 — HashMap
    },
    {
        "query": "퀵정렬 pivot partition 구현",
        "relevant_note_ids": ["note-{NOTE_5_ID}"],  # Note 5 — quickSort
    },
    {
        "query": "연결 리스트 Node 클래스 addFirst addLast",
        "relevant_note_ids": ["note-{NOTE_6_ID}"],  # Note 6 — LinkedList
    },
    {
        "query": "DFS 깊이 우선 탐색 스택 재귀 구현",
        "relevant_note_ids": ["note-{NOTE_7_ID}"],  # Note 7 — dfs_recursive
    },
    {
        "query": "BFS 너비 우선 탐색 Queue 최단 경로",
        "relevant_note_ids": ["note-{NOTE_8_ID}"],  # Note 8 — BFS
    },
    {
        "query": "스택 push pop peek isEmpty 구현",
        "relevant_note_ids": ["note-{NOTE_9_ID}"],  # Note 9 — Stack
    },
    {
        "query": "동적 프로그래밍 배낭 문제 dp 배열 최적화",
        "relevant_note_ids": ["note-{NOTE_10_ID}"],  # Note 10 — Knapsack
    },
]

TOP_K = 4


def recall_at_k(retrieved_ids: list[str], relevant_ids: list[str], k: int = 4) -> float:
    retrieved_top_k = set(retrieved_ids[:k])
    relevant = set(relevant_ids)
    hits = len(retrieved_top_k & relevant)
    return hits / len(relevant) if relevant else 0.0


def run_experiment(collection_name: str, client: chromadb.HttpClient) -> tuple[float, list[dict]]:
    try:
        collection = client.get_collection(collection_name)
    except Exception as e:
        print(f"  [ERROR] 컬렉션 '{collection_name}' 를 찾을 수 없습니다: {e}")
        print(f"  → 해당 조건으로 노트를 임베딩했는지 확인하세요.")
        return 0.0, []

    results = []
    for q in QUERIES:
        # {NOTE_X_ID} placeholder 가 남아있으면 경고
        if "{NOTE_" in str(q["relevant_note_ids"]):
            print(f"  [WARN] Ground Truth ID 미입력: '{q['query']}' — 스크립트 상단의 QUERIES 딕셔너리에 실제 noteId를 채워주세요.")

        query_result = collection.query(
            query_texts=[q["query"]],
            n_results=TOP_K,
            include=["distances", "metadatas"],
        )

        retrieved_ids = query_result["ids"][0]          # list of document ids
        distances     = query_result["distances"][0]    # list of float

        recall = recall_at_k(retrieved_ids, q["relevant_note_ids"], TOP_K)
        avg_dist = sum(distances) / len(distances) if distances else 0.0

        results.append({
            "query":       q["query"],
            "recall":      recall,
            "avg_dist":    avg_dist,
            "retrieved":   retrieved_ids,
            "relevant":    q["relevant_note_ids"],
        })

    avg_recall = sum(r["recall"] for r in results) / len(results)
    return avg_recall, results


def print_report(label: str, avg_recall: float, results: list[dict]) -> None:
    print(f"\n{'='*60}")
    print(f" {label}")
    print(f"{'='*60}")
    print(f" Recall@{TOP_K} 평균: {avg_recall:.3f}  ({avg_recall*100:.1f}%)")
    print()
    for r in results:
        hit = "✓" if r["recall"] > 0 else "✗"
        print(f"  {hit} [{r['recall']:.2f}] avg_dist={r['avg_dist']:.4f}  \"{r['query']}\"")
        print(f"       retrieved: {r['retrieved']}")
        print(f"       relevant:  {r['relevant']}")


def main():
    print("ChromaDB 연결 중...")
    client = chromadb.HttpClient(host=CHROMA_HOST, port=CHROMA_PORT)

    print(f"\n[조건 A] 전처리 미적용 — '{WITHOUT_COLLECTION}'")
    recall_without, results_without = run_experiment(WITHOUT_COLLECTION, client)

    print(f"\n[조건 B] 전처리 적용 — '{WITH_COLLECTION}'")
    recall_with, results_with = run_experiment(WITH_COLLECTION, client)

    print_report("조건 A — 전처리 미적용 (test-without-preprocessor)", recall_without, results_without)
    print_report("조건 B — 전처리 적용   (test-with-preprocessor)",    recall_with,    results_with)

    diff = recall_with - recall_without
    print(f"\n{'='*60}")
    print(f" 결과 요약")
    print(f"{'='*60}")
    print(f"  Recall@{TOP_K} 차이: {diff:+.3f} ({diff*100:+.1f}%p)")
    if diff >= 0.1:
        print(f"  → 판정: ✅ 전처리 효과 확인됨 (기준: ≥ 10%p)")
    elif diff > 0:
        print(f"  → 판정: △ 소폭 향상 (기준 미달)")
    else:
        print(f"  → 판정: ✗ 전처리 효과 없음 또는 역효과")

    result_json = {
        "without_preprocessor": {"recall_at_4": recall_without, "details": results_without},
        "with_preprocessor":    {"recall_at_4": recall_with,    "details": results_with},
        "diff": diff,
    }
    with open("preprocessor_recall_result.json", "w", encoding="utf-8") as f:
        json.dump(result_json, f, ensure_ascii=False, indent=2)
    print(f"\n  결과 저장: preprocessor_recall_result.json")


if __name__ == "__main__":
    main()
