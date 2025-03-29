/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.esb.module.ai.memory.store;

import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryStoreHandler {

    private static final MemoryStoreHandler INSTANCE = new MemoryStoreHandler();
    private static final Map<String, ChatMemoryStore> CHAT_MEMORY_STORE_MAP = new ConcurrentHashMap<>();

    private MemoryStoreHandler() {

    }

    public static MemoryStoreHandler getMemoryStoreHandler() {

        return INSTANCE;
    }

    public void addMemoryStore(String connectionName, ChatMemoryStore memoryStore) {

        CHAT_MEMORY_STORE_MAP.computeIfAbsent(connectionName, k -> memoryStore);
    }

    public ChatMemoryStore getMemoryStore(String connectionName) {

        return CHAT_MEMORY_STORE_MAP.get(connectionName);
    }

    public void shutdownConnections() {

        CHAT_MEMORY_STORE_MAP.values().forEach((memoryStore) -> {
            if (memoryStore instanceof DatabaseChatMemoryStore) {
                ((DatabaseChatMemoryStore) memoryStore).shutdown();
            }
        });
    }
}
