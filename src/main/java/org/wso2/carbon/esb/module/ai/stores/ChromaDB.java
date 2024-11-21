package org.wso2.carbon.esb.module.ai.stores;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import org.wso2.carbon.esb.module.ai.models.TextEmbedding;

import java.util.ArrayList;
import java.util.List;

public class ChromaDB implements VectorStore {

    private final EmbeddingStore<TextSegment> embeddingStore;

    public ChromaDB(String name, String url, String collection) {
        this.embeddingStore = ChromaEmbeddingStore
                        .builder()
                        .baseUrl(url)
                        .collectionName(collection)
                        .build();
    }

    @Override
    public void add(List<TextEmbedding> textEmbeddings) {
        List<Embedding> embeddings = new ArrayList<>();
        List<TextSegment> textSegments = new ArrayList<>();
        for (TextEmbedding textEmbedding : textEmbeddings) {
            embeddings.add(new Embedding(textEmbedding.getEmbedding()));
            textSegments.add(new TextSegment(textEmbedding.getText(), textEmbedding.getMetadata()));
        }
        embeddingStore.addAll(embeddings, textSegments);
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> search(Embedding embedding, Integer maxResults, Double minScore, Filter filter) {
        EmbeddingSearchRequest embeddingSearchRequest = new EmbeddingSearchRequest(embedding, maxResults, minScore, filter);
        return embeddingStore.search(embeddingSearchRequest).matches();
    }
}
