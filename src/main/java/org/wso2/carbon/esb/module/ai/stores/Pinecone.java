package org.wso2.carbon.esb.module.ai.stores;

import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;

public class Pinecone extends VectorStore {
        public Pinecone(String apiKey, String namespace, String cloud, String region, String index, Integer dimension) {
                super(PineconeEmbeddingStore.builder()
                    .apiKey(apiKey)
                    .index(index)
                    .nameSpace(namespace)
                    .createIndex(PineconeServerlessIndexConfig.builder()
                            .cloud(cloud)
                            .region(region)
                            .dimension(dimension)
                            .build())
                    .build());
        }
}
