package org.wso2.carbon.esb.module.ai.stores;


import java.util.List;

public interface KnowledgeStore {
    void ingest(TextEmbedding textEmbedding);
    void ingestAll(List<TextEmbedding> textEmbeddings);
}
