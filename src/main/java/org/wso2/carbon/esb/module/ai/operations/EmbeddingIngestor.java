package org.wso2.carbon.esb.module.ai.operations;

import com.google.gson.*;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;
import org.wso2.carbon.esb.module.ai.stores.KnowledgeStore;
import org.wso2.carbon.esb.module.ai.stores.KnowledgeStoreConnectionHandler;
import org.wso2.carbon.esb.module.ai.models.TextEmbedding;

import java.util.ArrayList;
import java.util.List;

public class EmbeddingIngestor extends AbstractAIMediator {

    private static final Gson gson = new Gson();

    @Override
    public void initialize(MessageContext mc) {
    }

    @Override
    public void execute(MessageContext mc) {
        String connectionName = getProperty(mc, "connectionName", String.class, false);
        String embeddings = getMediatorParameter(mc, "input", String.class, false);
        String outputProperty = getMediatorParameter(mc, "outputProperty", String.class, false);

        List<TextEmbedding> textEmbeddings = parseAndValidateInput(embeddings);
        if (textEmbeddings == null) {
            handleException("Invalid input format. Expected a JSON array of TextEmbedding objects.", mc);
            return;
        }

        KnowledgeStore knowledgeStore = KnowledgeStoreConnectionHandler.getKnowledgeStore(connectionName, mc);
        try {
            knowledgeStore.ingestAll(textEmbeddings);
        } catch (Exception e) {
            handleException("Failed to ingest embedding", e, mc);
        } finally {
            mc.setProperty(outputProperty, "true");
        }
    }

    private List<TextEmbedding> parseAndValidateInput(String input) {
        try {
            JsonElement jsonElement = gson.fromJson(input, JsonElement.class);
            if (!jsonElement.isJsonArray()) {
                return null;
            }

            JsonArray jsonArray = jsonElement.getAsJsonArray();
            List<TextEmbedding> textEmbeddings = new ArrayList<>();
            for (JsonElement element : jsonArray) {
                if (element.isJsonObject()) {
                    TextEmbedding embedding = TextEmbedding.deserialize(element);
                    if (embedding.getText() != null && embedding.getEmbedding() != null) {
                        textEmbeddings.add(embedding);
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            }
            return textEmbeddings;
        } catch (JsonSyntaxException e) {
            return null;
        }
    }
}