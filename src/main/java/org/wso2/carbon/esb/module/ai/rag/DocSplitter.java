package org.wso2.carbon.esb.module.ai.rag;

import com.google.gson.Gson;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;
import java.util.List;

public class DocSplitter extends AbstractAIMediator {

    @Override
    public void initialize(MessageContext mc) {
    }

    @Override
    public void execute(MessageContext mc) {
        String input = getMediatorParameter(mc, "input", String.class, false);
        String strategy = getMediatorParameter(mc, "strategy", String.class, false);
        String outputProperty = getMediatorParameter(mc, "outputProperty", String.class, false);

        DocumentSplitter splitter = DocumentSplitters.recursive(1000, 200, new OpenAiTokenizer());
        List<String> segments = splitter.split(new Document(input)).stream().map(TextSegment::text).toList();

        Gson gson = new Gson();
        String jsonSegments = gson.toJson(segments);

        mc.setProperty(outputProperty, jsonSegments);
    }
}
