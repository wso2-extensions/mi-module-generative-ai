package org.wso2.carbon.esb.module.ai.operations;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EmbeddingGenerator extends AbstractAIMediator {

    private static final Gson gson = new Gson();

    @Override
    public void initialize(MessageContext mc) {
    }

    @Override
    public void execute(MessageContext mc) {
        String input = getMediatorParameter(mc, "input", String.class, false);
        String model = getMediatorParameter(mc, "model", String.class, false);
        String outputProperty = getMediatorParameter(mc, "outputProperty", String.class, false);
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
                textEmbeddings.add(new TextEmbedding(inputs.get(i).text(), embedding.content().get(i).vector(), new Metadata()));
            }
        } catch (Exception e) {
            handleException("Failed to generate embedding", e, mc);
        }

        // If input is a single string, return a single TextEmbedding object
        // Otherwise, return a JSON array of TextEmbedding objects
        if (textEmbeddings.size() == 1) {
            mc.setProperty(outputProperty, gson.toJson(textEmbeddings.get(0)));
            return;
        }
        mc.setProperty(outputProperty, gson.toJson(textEmbeddings));
    }

    private List<TextSegment> parseAndValidateInput(String input) {
        List<TextSegment> textSegments = new ArrayList<>();
        try {
            // Try to parse input as a JSON array of TextSegment objects
            Type listType = new TypeToken<List<TextSegment>>() {}.getType();
            textSegments = gson.fromJson(input, listType);

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
