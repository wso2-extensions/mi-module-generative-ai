package org.wso2.carbon.esb.module.ai.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;

import java.util.concurrent.ConcurrentHashMap;

public class CreateOrLoadInMemoryStore extends AbstractAIMediator {

    private static final ConcurrentHashMap<String, InMemoryEmbeddingStore<TextSegment>> storeCache = new ConcurrentHashMap<>();

    @Override
    public void execute(MessageContext mc) {
        String storeName = "rag_store_" + getMediatorParameter(mc, "storeName", String.class, false);
        mc.setProperty(storeName, storeCache.computeIfAbsent(storeName, k -> new InMemoryEmbeddingStore<>()));
    }
}

