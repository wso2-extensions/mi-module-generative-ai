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

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.micro.integrator.registry.MicroIntegratorRegistry;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileMemoryStore implements ChatMemoryStore {

    public static final String CHAT_MEMORY_STORE = "ai/chat-memory/";
    public static final String JSON = ".json";
    protected Log log = LogFactory.getLog(this.getClass());
    public static final String CONTENT_TYPE = "application/json";
    private final MicroIntegratorRegistry registry;
    private final String STORE_FILE;
    private final FileChatMemoryStoreJsonCodec codec = new FileChatMemoryStoreJsonCodec();
    private final Map<String, List<ChatMessage>> chatMessages = new ConcurrentHashMap<>();

    public FileMemoryStore(String name, MicroIntegratorRegistry registry) throws IOException {

        this.registry = registry;
        STORE_FILE = CHAT_MEMORY_STORE + name + JSON;
        if (registry.isResourceExists(STORE_FILE)) {
            Map<String, List<ChatMessage>> loadedMessages = codec.fromFile(getStoreFile());
            if (loadedMessages != null) {
                chatMessages.putAll(loadedMessages);
            }
        }
    }

    private File getStoreFile() {

        return new File(registry.getRegistryEntry(STORE_FILE).getName());
    }

    @Override
    public List<ChatMessage> getMessages(Object sessionId) {

        return new ArrayList<>(chatMessages.getOrDefault(sessionId, new ArrayList<>()));
    }

    @Override
    public void updateMessages(Object sessionId, List<ChatMessage> messages) {

        chatMessages.put((String) sessionId, new ArrayList<>(messages));
        try {
            persistStoreToFile();
        } catch (IOException e) {
            log.error("Failed to persist chat memory to file", e);
        }
    }

    @Override
    public void deleteMessages(Object sessionId) {

        chatMessages.remove(sessionId);
        try {
            persistStoreToFile();
        } catch (IOException e) {
            log.error("Failed to persist chat memory to file", e);
        }
    }

    private void persistStoreToFile() throws IOException {

        String serialized = codec.serializeToJson(chatMessages);
        registry.addMultipartResource(STORE_FILE, CONTENT_TYPE, serialized.getBytes());
    }
}
