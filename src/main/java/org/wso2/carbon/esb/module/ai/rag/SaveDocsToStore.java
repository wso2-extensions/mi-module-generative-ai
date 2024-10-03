package org.wso2.carbon.esb.module.ai.rag;

import dev.langchain4j.data.document.Document;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;

import java.util.List;

public class SaveDocsToStore extends AbstractAIMediator {

    @Override
    public void execute(MessageContext mc){
        log.info("Executing SaveDocsToStore mediator");

        String storeName= "VECTOR_STORE_" + getMediatorParameter(mc, "storeName", String.class, false);
        Object store = getObjetFromMC(mc, storeName, false);
        KnowledgeStore knowledgeStore = (KnowledgeStore) store;

        String docLoader = getMediatorParameter(mc, "docLoader", String.class, false);

        List<Document> documents = getDocuments(mc, "DOC_LOADER_" + docLoader);
        knowledgeStore.ingestDocuments(documents);

        knowledgeStore.serializeToJson("/Users/isuruWij/wso2/LowCodeAIBuilder/docs/store.json");

        log.info("Ingesting documents to store: " + storeName);
    }

    @SuppressWarnings("unchecked")
    private List<Document> getDocuments(MessageContext mc, String documentsPropertyName) {
        Object docs = getObjetFromMC(mc, documentsPropertyName, false);
        return (List<Document>) docs;
    }
}
