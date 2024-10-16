package org.wso2.carbon.esb.module.ai.operations;

import dev.langchain4j.data.embedding.Embedding;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;
import org.wso2.carbon.esb.module.ai.stores.KnowledgeStore;

public class EmbeddingStoreRetriever extends AbstractAIMediator {

    KnowledgeStore knowledgeStore;
    Embedding queryEmbedding;
    String output;

    @Override
    public void initialize(MessageContext mc) {
    }

    @Override
    public void execute(MessageContext mc) {

    }
}

