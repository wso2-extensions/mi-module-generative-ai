package org.wso2.carbon.esb.module.ai.operations;

import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;


public class DocParser extends AbstractAIMediator {

    @Override
    public void initialize(MessageContext mc) {
    }

    @Override
    public void execute(MessageContext mc) {
        String input = getMediatorParameter(mc, "input", String.class, false);
        String parserType = getMediatorParameter(mc, "parser", String.class, false);
        String outputProperty = getMediatorParameter(mc, "outputProperty", String.class, false);

        DocumentParser parser = null;
        switch (parserType) {
            case "TEXT":
                parser = new TextDocumentParser();
                break;
            case "PDF":
                // PDF parser is not implemented yet
            default:
                handleException("Invalid parser type: " + parserType, mc);
        }

        String document = parseDocument(input, Objects.requireNonNull(parser, "Parser is not initialized"));
        mc.setProperty(outputProperty, document);
    }

    private String parseDocument(String input, DocumentParser parser) {
        InputStream stream = new ByteArrayInputStream(input.getBytes());
        return parser.parse(stream).text();
    }
}
