package org.wso2.carbon.esb.module.ai.operations;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.FilterParser;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;
import org.wso2.carbon.esb.module.ai.utils.StringFilterParser;
import org.wso2.carbon.esb.module.ai.models.TextEmbedding;
import org.wso2.carbon.esb.module.ai.stores.KnowledgeStore;
import org.wso2.carbon.esb.module.ai.stores.KnowledgeStoreConnectionHandler;

import java.util.List;

public class EmbeddingStoreRetriever extends AbstractAIMediator {

    FilterParser filterParser = new StringFilterParser();
    private static final Gson gson = new Gson();

    @Override
    public void initialize(MessageContext mc) {
    }

    @Override
    public void execute(MessageContext mc) {
        String connectionName = getProperty(mc, "connectionName", String.class, false);
        String input = getMediatorParameter(mc, "input", String.class, false);
        Integer maxResults = getMediatorParameter(mc, "maxResults", Integer.class, false);
        Double minScore = getMediatorParameter(mc, "minScore", Double.class, false);
        String filterString = getMediatorParameter(mc, "filter", String.class, true);
        String outputProperty = getMediatorParameter(mc, "outputProperty", String.class, false);

        TextEmbedding textEmbedding = parseAndValidateInput(input);
        if (textEmbedding == null) {
            handleException("Invalid input format. Expected a JSON object of TextEmbedding.", mc);
            return;
        }

        Filter filter = null;
        try {
            filter = filterParser.parse(filterString);
        } catch (IllegalArgumentException e) {
            handleException("Failed to parse filter", e, mc);
            return;
        }

        KnowledgeStore knowledgeStore = KnowledgeStoreConnectionHandler.getKnowledgeStore(connectionName, mc);
        try {
            Embedding embedding = new Embedding(textEmbedding.getEmbedding());
            List<EmbeddingMatch<TextSegment>> matches = knowledgeStore.search(embedding, maxResults, minScore, filter);
            String jsonMatches = gson.toJson(matches);
            mc.setProperty(outputProperty, jsonMatches);
        } catch (Exception e) {
            handleException("Failed to retrieve embedding", e, mc);
        }
    }

    private TextEmbedding parseAndValidateInput(String input) {
        try {
            JsonElement jsonElement = gson.fromJson(input, JsonElement.class);
            if (jsonElement.isJsonObject()) {
                TextEmbedding embedding = TextEmbedding.deserialize(jsonElement);
                if (embedding.getText() != null && embedding.getEmbedding() != null) {
                    return embedding;
                }
            }
            return null;
        } catch (JsonSyntaxException e) {
            return null;
        }
    }
}

