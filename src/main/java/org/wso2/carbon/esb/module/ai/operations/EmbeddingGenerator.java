package org.wso2.carbon.esb.module.ai.operations;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;
import org.wso2.carbon.esb.module.ai.llm.LLMConnectionHandler;
import org.wso2.carbon.esb.module.ai.models.TextEmbedding;
import org.wso2.carbon.esb.module.ai.utils.Utils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


/**
 *  Embedding generation operation
 *
 *  Inputs:
 *  - input: String or JSON array of Strings objects
 *  - model: Name of the embedding model
 *  - responseVariable: Variable name to store the output
 *  - connectionName: Name of the connection to the LLM
 *
 *  Outputs:
 *  - TextEmbedding object or JSON array of TextEmbedding objects
 */
public class EmbeddingGenerator extends AbstractAIMediator {

    @Override
    public void initialize(MessageContext mc) {
    }

    @Override
    public void execute(MessageContext mc) {

        String input = getMediatorParameter(mc, "input", String.class, false);
        String model = getMediatorParameter(mc, "model", String.class, false);
        String responseVariable = getMediatorParameter(mc, "responseVariable", String.class, false);
        String connectionName = getProperty(mc, "connectionName", String.class, false);

        List<TextSegment> inputs = parseAndValidateInput(input);
        if (inputs == null) {
            handleException("Invalid input format. Expected a string or a JSON array of strings.", mc);
            return;
        }

        EmbeddingModel embeddingModel = LLMConnectionHandler.getEmbeddingModel(connectionName, model);
        List<TextEmbedding> textEmbeddings = new ArrayList<>();
        try {
            Response<List<Embedding>> embedding = embeddingModel.embedAll(inputs);
            for (int i = 0; i < inputs.size(); i++) {
                textEmbeddings.add(new TextEmbedding(inputs.get(i).text(), embedding.content().get(i).vector(), inputs.get(i).metadata()));
            }
        } catch (Exception e) {
            handleException("Failed to generate embedding", e, mc);
        }

        // If multiple inputs were provided, return a JSON array of TextEmbedding objects
        handleResponse(mc, responseVariable, textEmbeddings, null, null);
    }

    private List<TextSegment> parseAndValidateInput(String input) {
        List<TextSegment> textSegments = new ArrayList<>();
        try {
            // Try to parse input as a JSON array of TextSegment objects
            Type listType = new TypeToken<List<TextSegment>>() {}.getType();
            textSegments = Utils.fromJson(input, listType);

            // If parsing as JSON array fails, treat input as a single string
            if (textSegments == null || textSegments.isEmpty()) {
                textSegments = new ArrayList<>();
                textSegments.add(new TextSegment(input, new Metadata()));
            }

            for (TextSegment segment : textSegments) {
                if (segment.text() == null || segment.text().isEmpty()) {
                    return null;
                }
            }
        } catch (JsonSyntaxException e) {
            // If JSON parsing fails, treat input as a single string
            textSegments = new ArrayList<>();
            textSegments.add(new TextSegment(input, new Metadata()));
        }
        return textSegments;
    }
}
