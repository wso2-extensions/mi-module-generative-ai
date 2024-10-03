package org.wso2.carbon.esb.module.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

public class KnowledgeStore {
    private final String name;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStoreIngestor ingestor;
    private final DocumentSplitter documentSplitter;

    public KnowledgeStore(String name, EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        this.name = name;
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.documentSplitter = DocumentSplitters.recursive(1000, 200, new OpenAiTokenizer());
        this.ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(documentSplitter)
                .textSegmentTransformer(textSegment -> TextSegment.from(
                        textSegment.metadata().getString("file_name") + "\n" + textSegment.text(),
                        textSegment.metadata()
                ))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
    }

    public String getName() {
        return name;
    }

    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        return embeddingStore;
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }

    public void ingestDocuments(List<Document> documents) {
        ingestor.ingest(documents);
    }

    public void ingestDocument(Document document) {
        ingestor.ingest(document);
    }

    public void serializeToJson(String path) {
        // Throw error if embeddingStore is not InMemoryEmbeddingStore
        if (!(embeddingStore instanceof InMemoryEmbeddingStore)) {
            throw new RuntimeException("Only InMemoryEmbeddingStore can be serialized");
        }
        InMemoryEmbeddingStore<TextSegment> embeddingStore = (InMemoryEmbeddingStore<TextSegment>) this.embeddingStore;
        String serializedStore = embeddingStore.serializeToJson();
        InMemoryEmbeddingStore<TextSegment> deserializedStore = InMemoryEmbeddingStore.fromJson(serializedStore);
        embeddingStore.serializeToFile(path);
    }
}
