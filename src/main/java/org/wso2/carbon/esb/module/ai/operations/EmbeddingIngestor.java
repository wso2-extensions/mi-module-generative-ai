package org.wso2.carbon.esb.module.ai.operations;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;
import org.wso2.carbon.esb.module.ai.stores.VectorStore;
import org.wso2.carbon.esb.module.ai.stores.VectorStoreConnectionHandler;
import org.wso2.carbon.esb.module.ai.models.TextEmbedding;

import java.lang.reflect.Type;
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

        VectorStore vectorStore = VectorStoreConnectionHandler.getVectorStore(connectionName, mc);
        try {
            vectorStore.add(textEmbeddings);
        } catch (Exception e) {
            handleException("Failed to ingest embedding", e, mc);
        } finally {
            mc.setProperty(outputProperty, "true");
        }
    }

    private List<TextEmbedding> parseAndValidateInput(String input) {
        try {
            Type listType = new TypeToken<List<TextEmbedding>>() {}.getType();
            List<TextEmbedding> textEmbeddings = gson.fromJson(input, listType);
            return validateTextEmbeddings(textEmbeddings);
        } catch (JsonSyntaxException e) {
            try {
                TextEmbedding singleEmbedding = gson.fromJson(input, TextEmbedding.class);
                List<TextEmbedding> textEmbeddings = new ArrayList<>();
                textEmbeddings.add(singleEmbedding);
                return validateTextEmbeddings(textEmbeddings);
            } catch (JsonSyntaxException ex) {
                return null;
            }
        }
    }

    private List<TextEmbedding> validateTextEmbeddings(List<TextEmbedding> textEmbeddings) {
        if (textEmbeddings != null) {
            for (TextEmbedding embedding : textEmbeddings) {
                if (embedding.getText() == null || embedding.getText().isEmpty() ||
                        embedding.getEmbedding() == null || embedding.getEmbedding().length == 0) {
                    return null;
                }
            }
        }
        return textEmbeddings;
    }
}
