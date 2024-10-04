package org.wso2.carbon.esb.module.ai.rag;

import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.Document;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;

import java.util.ArrayList;
import java.util.List;

public class DocumentLoader extends AbstractAIMediator {

    String name;
    String source;
    String directory;
    String document;

    @Override
    public void init(MessageContext mc) {
        name = getMediatorParameter(mc, "name", String.class, false);
        source = getMediatorParameter(mc, "source", String.class, false);

        switch (source) {
            case "Local-directory":
                directory = getMediatorParameter(mc, "directory", String.class, false);
                break;
            case "Local-file":
                document = getMediatorParameter(mc, "document", String.class, false);
                break;
            case "File-inbound-endpoint":
                break;
            default:
                throw new IllegalArgumentException("Invalid document source: " + source);
        }
    }

    @Override
    public void execute(MessageContext mc) {
        List<Document> documents = new ArrayList<>();
        switch (source) {
            case "Local-directory":
                documents.addAll(FileSystemDocumentLoader.loadDocumentsRecursively(directory, new TextDocumentParser()));
                break;
            case "Local-file":
                documents.add(FileSystemDocumentLoader.loadDocument(document, new TextDocumentParser()));
                break;
            case "File-inbound-endpoint":
                String text = mc.getEnvelope().getBody().getFirstElement().getText();
                Document doc = new Document(text);
                documents.add(doc);
                break;
            default:
                throw new IllegalArgumentException("Invalid document loader type: " + source);
        }
        mc.setProperty("DOC_LOADER_" + name, documents);
        log.info("Loaded " + documents.size() + " documents");
    }
}
