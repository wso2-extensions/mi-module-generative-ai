package org.wso2.carbon.esb.module.ai.rag;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;
import org.wso2.micro.integrator.registry.MicroIntegratorRegistry;

import java.util.concurrent.ConcurrentHashMap;

public class InitKnowledgeStore extends AbstractAIMediator {

    private static final ConcurrentHashMap<String, KnowledgeStore> storeCache = new ConcurrentHashMap<>();
    private String storeName;
    private String type;

    @Override
    public void initialize(MessageContext mc) {
        storeName = getMediatorParameter(mc, "name", String.class, false);
        type = getMediatorParameter(mc, "type", String.class, false);
    }

    @Override
    public void execute(MessageContext mc) {
        mc.setProperty("VECTOR_STORE_" + storeName, createKnowledgeStore(mc));
    }

    private KnowledgeStore createKnowledgeStore(MessageContext mc) {
        String key = storeName + "|" + type;
        switch (type) {
            case "In-Memory":
                return storeCache.computeIfAbsent(key, k -> createInMemoryStore(mc));
            case "Pine-cone":
                //
            default:
                handleException("Unknown store type: " + type, mc);
                return null;
        }
    }

    private KnowledgeStore createInMemoryStore(MessageContext mc) {
        String apiKey = getProperty(mc, "ai_openai_apiKey", String.class, false);
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .build();
        MicroIntegratorRegistry microIntegratorRegistry =
                (MicroIntegratorRegistry) mc.getConfiguration().getRegistry();
        return new InMemoryKnowledgeStore(storeName, embeddingModel, microIntegratorRegistry);
    }
}

