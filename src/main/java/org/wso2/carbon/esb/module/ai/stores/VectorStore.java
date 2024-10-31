package org.wso2.carbon.esb.module.ai.stores;


import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.filter.Filter;
import org.wso2.carbon.esb.module.ai.models.TextEmbedding;

import java.util.List;

public interface VectorStore {
    void add(List<TextEmbedding> textEmbeddings);
    List<EmbeddingMatch<TextSegment>> search(Embedding embedding, Integer maxResults, Double minScore, Filter filter);
}
