package org.wso2.carbon.esb.module.ai.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;
import org.wso2.carbon.esb.module.ai.connections.LLMConnectionHandler;

public class EmbeddingGenerator extends AbstractAIMediator {

    @Override
    public void initialize(MessageContext mc) {
    }

    @Override
    public void execute(MessageContext mc) {
        String input = getMediatorParameter(mc, "input", String.class, false);
        String model = getMediatorParameter(mc, "model", String.class, false);
        String outputProperty = getMediatorParameter(mc, "outputProperty", String.class, false);
        String connectionName = getProperty(mc, "connectionName", String.class, false);

        EmbeddingModel embeddingModel = LLMConnectionHandler.getEmbeddingModel(connectionName, model);
        Response<Embedding> embedding;
        try {
            embedding = embeddingModel.embed(input);
        } catch (Exception e) {
            // stack trace
            throw new RuntimeException("Failed to generate embedding", e);
        }

        mc.setProperty(outputProperty, embedding.toString());
    }
}
