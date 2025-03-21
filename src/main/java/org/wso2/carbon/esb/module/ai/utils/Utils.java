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

package org.wso2.carbon.esb.module.ai.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import java.lang.reflect.Type;

public class Utils {

    protected static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Metadata.class, new MetadataSerializer())
            .registerTypeAdapter(Metadata.class, new MetadataDeserializer())
            .registerTypeAdapter(Embedding.class, new EmbeddingSerializer())
            .registerTypeAdapter(Embedding.class, new EmbeddingDeserializer())
            .create();

    public static <T> T fromJson(String json, Type type) {
        return gson.fromJson(json, type);
    }

    public static String toJson(Object object) {
        return gson.toJson(object);
    }
}
