package org.wso2.carbon.esb.module.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.wso2.micro.integrator.registry.MicroIntegratorRegistry;

import java.util.List;

public class InMemoryKnowledgeStore implements KnowledgeStore {

    public static final String AI_KNOWLEDGE_STORE = "ai/knowledge-store/";
    public static final String JSON = ".json";
    public static final String CONTENT_TYPE = "application/json";
    private static MicroIntegratorRegistry registry;

    private final String name;

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStoreIngestor ingestor;

    private final String STORE_FILE;

    public InMemoryKnowledgeStore(String name, EmbeddingModel embeddingModel, MicroIntegratorRegistry registry) {
        this.name = name;
        this.embeddingModel = embeddingModel;
        InMemoryKnowledgeStore.registry = registry;

        STORE_FILE = AI_KNOWLEDGE_STORE + name + JSON;
        if (registry.isResourceExists(STORE_FILE)) {
            this.embeddingStore = InMemoryEmbeddingStore.fromFile(registry.getRegistryEntry(STORE_FILE).getName());
        } else {
            this.embeddingStore = new InMemoryEmbeddingStore<>();
            persistStoreToRegistry();
        }

        this.ingestor = createIngestor(this.embeddingStore);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        return embeddingStore;
    }

    @Override
    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }

    @Override
    public void ingestDocuments(List<Document> documents) {
        synchronized (this) {
            ingestor.ingest(documents);
            persistStoreToRegistry();
        }
    }

    private synchronized void persistStoreToRegistry() {
        InMemoryEmbeddingStore<TextSegment> embeddingStore = (InMemoryEmbeddingStore<TextSegment>) this.embeddingStore;
        String serializedStore = embeddingStore.serializeToJson();
        registry.addMultipartResource(STORE_FILE, CONTENT_TYPE, serializedStore.getBytes());
    }

    private EmbeddingStoreIngestor createIngestor(EmbeddingStore<TextSegment> embeddingStore) {
        return EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(1000, 200, new OpenAiTokenizer()))
                .textSegmentTransformer(textSegment -> TextSegment.from(
                        textSegment.metadata().getString("file_name") + "\n" + textSegment.text(),
                        textSegment.metadata()
                ))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
    }
}
