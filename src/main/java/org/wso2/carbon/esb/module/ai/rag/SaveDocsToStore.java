package org.wso2.carbon.esb.module.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SaveDocsToStore extends AbstractAIMediator {

    private static final ConcurrentHashMap<String, EmbeddingStoreIngestor> ingestorCache = new ConcurrentHashMap<>();

    @Override
    public void execute(MessageContext mc){
        log.info("Executing SaveDocsToStore mediator");
        String storeName= "rag_store_" + getMediatorParameter(mc, "storeName", String.class, false);
        String documentsPropertyName = getMediatorParameter(mc, "documentsPropertyName", String.class, false);

        EmbeddingStoreIngestor ingestor = ingestorCache.computeIfAbsent(
                storeName, k -> createEmbeddingStoreIngestor(mc, storeName));

        List<Document> documents = getDocuments(mc, documentsPropertyName);
        ingestor.ingest(documents);

        log.info("Ingesting documents to store: " + storeName);
    }


    @SuppressWarnings("unchecked")
    private EmbeddingStoreIngestor createEmbeddingStoreIngestor(MessageContext mc, String storeName) {
        Object store = getObjetFromMC(mc, storeName, false);
        String apiKey = getProperty(mc, "ai_openai_apiKey", String.class, false);

        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .build();
        InMemoryEmbeddingStore<TextSegment> embeddingStore = (InMemoryEmbeddingStore<TextSegment>) store;

        return EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(1000, 200, new OpenAiTokenizer()))
                .textSegmentTransformer(textSegment -> TextSegment.from(
                        textSegment.metadata("file_name") + "\n" + textSegment.text(),
                        textSegment.metadata()
                ))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<Document> getDocuments(MessageContext mc, String documentsPropertyName) {
        Object docs = getObjetFromMC(mc, documentsPropertyName, false);
        return (List<Document>) docs;
    }
}
