/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.esb.module.ai.stores;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import org.wso2.carbon.esb.module.ai.models.TextEmbedding;

import java.util.ArrayList;
import java.util.List;

public abstract class VectorStore {

    private EmbeddingStore<TextSegment> embeddingStore;

    protected VectorStore(EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingStore = embeddingStore;
    }

    public void add(List<TextEmbedding> textEmbeddings) {
        List<Embedding> embeddings = new ArrayList<>();
        List<TextSegment> textSegments = new ArrayList<>();
        for (TextEmbedding textEmbedding : textEmbeddings) {
            embeddings.add(new Embedding(textEmbedding.getEmbedding()));
            textSegments.add(new TextSegment(textEmbedding.getText(), textEmbedding.getMetadata()));
        }
        embeddingStore.addAll(embeddings, textSegments);
    }

    public List<EmbeddingMatch<TextSegment>> search(Embedding embedding, Integer maxResults, Double minScore, Filter filter) {
        EmbeddingSearchRequest embeddingSearchRequest = new EmbeddingSearchRequest(embedding, maxResults, minScore, filter);
        return embeddingStore.search(embeddingSearchRequest).matches();
    }

    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        return embeddingStore;
    }

    public void setEmbeddingStore(EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingStore = embeddingStore;
    }
}
