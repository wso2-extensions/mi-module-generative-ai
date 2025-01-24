package org.wso2.carbon.esb.module.ai.operations;

import com.google.gson.JsonSyntaxException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.FilterParser;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;
import org.wso2.carbon.esb.module.ai.utils.StringFilterParser;
import org.wso2.carbon.esb.module.ai.stores.VectorStore;
import org.wso2.carbon.esb.module.ai.stores.VectorStoreConnectionHandler;
import org.wso2.carbon.esb.module.ai.utils.Utils;

import java.util.List;

/**
 * Embedding store retrieval operation
 *
 * Inputs:
 * - connectionName: Name of the connection to the vector store
 * - input: TextEmbedding object
 * - maxResults: Maximum number of results to return
 * - minScore: Minimum score for the results
 * - filter: Filter string
 * - responseVariable: Variable name to store the output
 *
 * Outputs:
 * - List of EmbeddingMatch objects
 */
public class EmbeddingStoreRetriever extends AbstractAIMediator {

    FilterParser filterParser = new StringFilterParser();

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
        String responseVariable = getMediatorParameter(mc, "responseVariable", String.class, false);

        Embedding embedding = parseAndValidateInput(input);
        if (embedding == null) {
            handleException("Invalid input format. Expected a vector ( Array of numbers )", mc);
            return;
        }

        Filter filter = null;
        // Filter parsing is yet to be implemented

        VectorStore vectorStore = VectorStoreConnectionHandler.getVectorStore(connectionName, mc);
        try {
            List<EmbeddingMatch<TextSegment>> matches = vectorStore.search(embedding, maxResults, minScore, null);
            handleResponse(mc, responseVariable, matches, null, null);
        } catch (Exception e) {
            handleException("Failed to retrieve embedding", e, mc);
        }
    }

    private Embedding parseAndValidateInput(String input) {
        try {
            float[] embeddingArray = Utils.fromJson(input, float[].class);
            if (embeddingArray == null || embeddingArray.length == 0) {
                return null;
            }
            return new Embedding(embeddingArray);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }
}
