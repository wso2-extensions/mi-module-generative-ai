package org.wso2.carbon.esb.module.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.List;

public interface KnowledgeStore {
    String getName();
    EmbeddingStore<TextSegment> getEmbeddingStore();
    EmbeddingModel getEmbeddingModel();
    void ingestDocuments(List<Document> documents);
}
