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
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.wso2.carbon.esb.module.ai.memory.MessageWindowChatMemoryWithDatabase;
import org.wso2.carbon.esb.module.ai.memory.store.DatabaseChatMemoryStore;
import org.wso2.carbon.esb.module.ai.memory.store.MemoryStoreHandler;

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

    /**
     * Returns the chat memory for the given user ID
     *
     * @param userID          User ID
     * @param memoryConfigKey Memory configuration key
     * @param maxChatHistory  Maximum chat history
     * @return Chat memory
     */
    public static ChatMemory getChatMemory(String userID, String memoryConfigKey, int maxChatHistory) {

        ChatMemory chatMemory;
        ChatMemoryStore chatMemoryStore = MemoryStoreHandler.getMemoryStoreHandler().getMemoryStore(memoryConfigKey);
        if (chatMemoryStore instanceof DatabaseChatMemoryStore) {
            chatMemory = MessageWindowChatMemoryWithDatabase.builder().id(userID)
                    .chatMemoryStore((DatabaseChatMemoryStore) chatMemoryStore).maxMessages(maxChatHistory).build();
        } else {
            chatMemory = MessageWindowChatMemory.builder().id(userID).chatMemoryStore(chatMemoryStore)
                    .maxMessages(maxChatHistory).build();
        }
        return chatMemory;
    }
}
