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
import org.apache.synapse.registry.RegistryEntry;
import org.wso2.micro.integrator.registry.MicroIntegratorRegistry;

import java.util.List;

public class KnowledgeStore {

    public static final String AI_KNOWLEDGE_STORE = "ai/knowledge-store/";
    public static final String JSON = ".json";
    public static final String CONTENT_TYPE = "application/json";
    private static MicroIntegratorRegistry registry;

    private final String name;
    private final String type;

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStoreIngestor ingestor;
    private final DocumentSplitter documentSplitter;

    public KnowledgeStore(String name, String type, EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel, MicroIntegratorRegistry registry) {
        this.name = name;
        this.type = type;
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
        KnowledgeStore.registry = registry;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        final RegistryEntry registryEntry = registry.getRegistryEntry(AI_KNOWLEDGE_STORE + name + JSON);
        if (registryEntry != null) {
            // TODO: Create new embedding store only if the file has changed. Use the last modified time of the file.
            String path = registryEntry.getName();
            return InMemoryEmbeddingStore.fromFile(path);
        }
        return null;
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }

    public void ingestDocuments(List<Document> documents) {
        ingestor.ingest(documents);
        if (embeddingStore instanceof InMemoryEmbeddingStore) {
            persist();
        }
    }

    public void ingestDocument(Document document) {
        ingestor.ingest(document);
        if (embeddingStore instanceof InMemoryEmbeddingStore) {
            persist();
        }
    }

    private synchronized void persist() {
        InMemoryEmbeddingStore<TextSegment> embeddingStore = (InMemoryEmbeddingStore<TextSegment>) this.embeddingStore;
        String serializedStore = embeddingStore.serializeToJson();
        registry.addMultipartResource(AI_KNOWLEDGE_STORE + name + JSON, CONTENT_TYPE, serializedStore.getBytes());
    }
}
