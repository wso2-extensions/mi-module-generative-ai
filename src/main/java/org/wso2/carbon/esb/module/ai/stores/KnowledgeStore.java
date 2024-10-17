package org.wso2.carbon.esb.module.ai.stores;


import org.wso2.carbon.esb.module.ai.models.TextEmbedding;

import java.util.List;

public interface KnowledgeStore {
    void ingest(TextEmbedding textEmbedding);
    void ingestAll(List<TextEmbedding> textEmbeddings);
}
