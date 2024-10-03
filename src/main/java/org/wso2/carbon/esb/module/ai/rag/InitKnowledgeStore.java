package org.wso2.carbon.esb.module.ai.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;

import java.util.concurrent.ConcurrentHashMap;

public class InitKnowledgeStore extends AbstractAIMediator {

    private static final ConcurrentHashMap<String, KnowledgeStore> storeCache = new ConcurrentHashMap<>();

    @Override
    public void execute(MessageContext mc) {
        String storeName = getMediatorParameter(mc, "storeName", String.class, false);
        mc.setProperty("VECTOR_STORE_" + storeName, storeCache.computeIfAbsent(storeName, k -> {
            String apiKey = getProperty(mc, "ai_openai_apiKey", String.class, false);
            EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                    .apiKey(apiKey)
                    .build();
            InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
            return new KnowledgeStore(storeName, embeddingStore, embeddingModel);
        }));
    }
}

