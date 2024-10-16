package org.wso2.carbon.esb.module.ai.connections;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

public interface KnowledgeStore {
    String getName();
    EmbeddingStore<TextSegment> getEmbeddingStore();
    void ingest(Embedding embedding, TextSegment segment);
}
