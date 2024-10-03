package org.wso2.carbon.esb.module.ai.rag;

import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.Document;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;

import java.util.List;

public class DocumentLoader extends AbstractAIMediator {

    String documentsPath;
    String docLoaderName;

    @Override
    public void init(MessageContext mc) {
        documentsPath = getMediatorParameter(mc, "documentsPath", String.class, false);
        docLoaderName = getMediatorParameter(mc, "docLoaderName", String.class, false);
    }

    @Override
    public void execute(MessageContext mc) {
        List<Document> documents = FileSystemDocumentLoader.loadDocumentsRecursively(documentsPath, new TextDocumentParser());
        mc.setProperty("DOC_LOADER_" + docLoaderName, documents);
    }
}
