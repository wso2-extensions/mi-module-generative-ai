/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Milvus extends VectorStore {

    private static final Log log = LogFactory.getLog(Milvus.class);

    public Milvus(String uri, String token, String collectionName, int dimension) {

        super(buildStore(uri, token, collectionName, dimension));
    }

    private static MilvusEmbeddingStore buildStore(String uri, String token, String collectionName, int dimension) {

        var builder = MilvusEmbeddingStore.builder()
                .collectionName(collectionName)
                .dimension(dimension);

        if (uri != null && !uri.isEmpty()) {
            builder.uri(uri);
        }

        if (token != null && !token.isEmpty()) {
            builder.token(token);
        }

        return builder.build();
    }
}
