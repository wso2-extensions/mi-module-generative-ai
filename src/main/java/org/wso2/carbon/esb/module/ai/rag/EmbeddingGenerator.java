package org.wso2.carbon.esb.module.ai.rag;

import com.google.gson.Gson;
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
        float[] vector = null;
        try {
            Response<Embedding> embedding = embeddingModel.embed(input);
            vector = embedding.content().vector();
        } catch (Exception e) {
            // stack trace
            throw new RuntimeException("Failed to generate embedding", e);
        }

        // Convert the float array to a JSON string
        Gson gson = new Gson();
        String jsonVector = gson.toJson(vector);

        mc.setProperty(outputProperty, jsonVector);
    }
}
