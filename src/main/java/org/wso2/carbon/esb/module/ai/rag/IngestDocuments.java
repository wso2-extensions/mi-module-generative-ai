package org.wso2.carbon.esb.module.ai.rag;

import dev.langchain4j.data.document.Document;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;

import java.util.List;

public class IngestDocuments extends AbstractAIMediator {

    private KnowledgeStore knowledgeStore;
    private String docLoader;

    @Override
    public void initialize(MessageContext mc) {
        String storeName = "VECTOR_STORE_" + getMediatorParameter(mc, "storeName", String.class, false);
        Object store = getObjetFromMC(mc, storeName, false);
        knowledgeStore = (KnowledgeStore) store;
        docLoader = getMediatorParameter(mc, "docLoader", String.class, false);
    }

    @Override
    public void execute(MessageContext mc){
        List<Document> documents = getDocuments(mc, "DOC_LOADER_" + docLoader);
        knowledgeStore.ingestDocuments(documents);
        log.info("Ingested " + documents.size() + " documents");
    }

    @SuppressWarnings("unchecked")
    private List<Document> getDocuments(MessageContext mc, String documentsPropertyName) {
        Object docs = getObjetFromMC(mc, documentsPropertyName, false);
        return (List<Document>) docs;
    }
}
