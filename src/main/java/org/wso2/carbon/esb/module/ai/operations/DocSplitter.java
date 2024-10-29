package org.wso2.carbon.esb.module.ai.operations;

import com.google.gson.Gson;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;

import java.util.List;
import java.util.Objects;

public class DocSplitter extends AbstractAIMediator {

    private static final Gson gson = new Gson();

    @Override
    public void initialize(MessageContext mc) {
    }

    @Override
    public void execute(MessageContext mc) {
        String input = getMediatorParameter(mc, "input", String.class, false);
        String strategy = getMediatorParameter(mc, "strategy", String.class, false);
        Integer maxSegmentSize = getMediatorParameter(mc, "maxSegmentSize", Integer.class, true);
        Integer maxOverlapSize = getMediatorParameter(mc, "minSegmentSize", Integer.class, true);
        String outputProperty = getMediatorParameter(mc, "outputProperty", String.class, false);

        maxSegmentSize = (maxSegmentSize == null) ? 1000 : maxSegmentSize;
        maxOverlapSize = (maxOverlapSize == null) ? 200 : maxOverlapSize;

        DocumentSplitter splitter = null;
        switch (strategy) {
            case "Recursive":
                splitter = DocumentSplitters.recursive(maxSegmentSize, maxOverlapSize, new OpenAiTokenizer());
                break;
            case "ByParagraph":
                splitter = new DocumentByParagraphSplitter(maxSegmentSize, maxOverlapSize, new OpenAiTokenizer());
                break;
            case "BySentence":
                splitter = new DocumentBySentenceSplitter(maxSegmentSize, maxOverlapSize, new OpenAiTokenizer());
                break;
            default:
                handleException("Invalid strategy: " + strategy, mc);
        }

        List<TextSegment> segments = Objects.requireNonNull(splitter).split(new Document(input));

        String jsonSegments = gson.toJson(segments);
        mc.setProperty(outputProperty, jsonSegments);
    }
}