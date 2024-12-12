package org.wso2.carbon.esb.module.ai.stores;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.wso2.carbon.esb.module.ai.models.TextEmbedding;
import org.wso2.micro.integrator.registry.MicroIntegratorRegistry;

import java.util.List;

public class MIVectorStore extends VectorStore {

    public static final String AI_VECTOR_STORE = "ai/vector-store/";
    public static final String JSON = ".json";
    public static final String CONTENT_TYPE = "application/json";

    private static Boolean PERSISTENCE_ENABLED = false;
    private static MicroIntegratorRegistry registry;

    private final String STORE_FILE;

    public MIVectorStore(String name, Boolean enablePersistence, MicroIntegratorRegistry registry) {
        super(new InMemoryEmbeddingStore<>());

        PERSISTENCE_ENABLED = enablePersistence;
        MIVectorStore.registry = registry;
        STORE_FILE = AI_VECTOR_STORE + name + JSON;
        if ( PERSISTENCE_ENABLED && registry.isResourceExists(STORE_FILE)) {
            EmbeddingStore<TextSegment> embeddingStore = InMemoryEmbeddingStore.fromFile(registry.getRegistryEntry(STORE_FILE).getName());
            super.setEmbeddingStore(embeddingStore);
        }
    }

    @Override
    public void add(List<TextEmbedding> textEmbeddings) {
        synchronized (this) {
            super.add(textEmbeddings);
            persistStoreToRegistry();
        }
    }

    private synchronized void persistStoreToRegistry() {
        if (!PERSISTENCE_ENABLED) {
            return;
        }
        InMemoryEmbeddingStore<TextSegment> embeddingStore = (InMemoryEmbeddingStore<TextSegment>) getEmbeddingStore();
        String serializedStore = embeddingStore.serializeToJson();
        registry.addMultipartResource(STORE_FILE, CONTENT_TYPE, serializedStore.getBytes());
    }
}
