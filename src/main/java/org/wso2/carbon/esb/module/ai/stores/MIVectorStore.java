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

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.wso2.carbon.esb.module.ai.models.TextEmbedding;
import org.wso2.micro.integrator.registry.MicroIntegratorRegistry;

import java.util.List;

public class MIVectorStore extends VectorStore {

    public static final String AI_VECTOR_STORE = "ai/vector-store/";
    public static final String JSON = ".json";
    public static final String CONTENT_TYPE = "application/json";

    private static MicroIntegratorRegistry registry;
    private final String STORE_FILE;

    public MIVectorStore(String name, MicroIntegratorRegistry registry) {

        super(new InMemoryEmbeddingStore<>());
        MIVectorStore.registry = registry;
        STORE_FILE = AI_VECTOR_STORE + name + JSON;
        if (registry.isResourceExists(STORE_FILE)) {
            EmbeddingStore<TextSegment> embeddingStore =
                    InMemoryEmbeddingStore.fromFile(registry.getRegistryEntry(STORE_FILE).getName());
            super.setEmbeddingStore(embeddingStore);
        }
    }

    @Override
    public void add(List<TextEmbedding> textEmbeddings) {

        synchronized (this) {
            super.add(textEmbeddings);
            persistStoreToRegistry();
        }
    }

    private synchronized void persistStoreToRegistry() {

        InMemoryEmbeddingStore<TextSegment> embeddingStore = (InMemoryEmbeddingStore<TextSegment>) getEmbeddingStore();
        String serializedStore = embeddingStore.serializeToJson();
        registry.addMultipartResource(STORE_FILE, CONTENT_TYPE, serializedStore.getBytes());
    }
}
