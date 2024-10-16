package org.wso2.carbon.esb.module.ai.connections;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.wso2.micro.integrator.registry.MicroIntegratorRegistry;

public class InMemoryKnowledgeStore implements KnowledgeStore {

    public static final String AI_KNOWLEDGE_STORE = "ai/knowledge-store/";
    public static final String JSON = ".json";
    public static final String CONTENT_TYPE = "application/json";
    private static MicroIntegratorRegistry registry;

    private final String name;

    private final EmbeddingStore<TextSegment> embeddingStore;

    private final String STORE_FILE;

    public InMemoryKnowledgeStore(String name, MicroIntegratorRegistry registry) {
        this.name = name;
        InMemoryKnowledgeStore.registry = registry;

        STORE_FILE = AI_KNOWLEDGE_STORE + name + JSON;
        if (registry.isResourceExists(STORE_FILE)) {
            this.embeddingStore = InMemoryEmbeddingStore.fromFile(registry.getRegistryEntry(STORE_FILE).getName());
        } else {
            this.embeddingStore = new InMemoryEmbeddingStore<>();
            persistStoreToRegistry();
        }
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
    public void ingest(Embedding embedding, TextSegment segment) {
        synchronized (this) {
            embeddingStore.add(embedding, segment);
            persistStoreToRegistry();
        }
    }

    private synchronized void persistStoreToRegistry() {
        InMemoryEmbeddingStore<TextSegment> embeddingStore = (InMemoryEmbeddingStore<TextSegment>) this.embeddingStore;
        String serializedStore = embeddingStore.serializeToJson();
        registry.addMultipartResource(STORE_FILE, CONTENT_TYPE, serializedStore.getBytes());
    }
}
