package org.wso2.carbon.esb.module.ai.stores;

import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

public class ChromaDB extends VectorStore {
    public ChromaDB(String url, String collection) {
        super(ChromaEmbeddingStore
                        .builder()
                        .baseUrl(url)
                        .collectionName(collection)
                        .build());
    }
}
