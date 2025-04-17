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

package org.wso2.carbon.esb.module.ai.memory;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.internal.ValidationUtils;
import dev.langchain4j.memory.ChatMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.esb.module.ai.memory.store.DatabaseChatMemoryStore;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * This class is a modified version of the {@link dev.langchain4j.memory.chat.MessageWindowChatMemory} class.
 */
public class MessageWindowChatMemoryWithDatabase implements ChatMemory {

    private Logger log = LoggerFactory.getLogger(MessageWindowChatMemoryWithDatabase.class);
    private final Object id;
    private final Integer maxMessages;
    private DatabaseChatMemoryStore store;

    private MessageWindowChatMemoryWithDatabase(Builder builder) {

        this.id = ValidationUtils.ensureNotNull(builder.id, "id");
        this.maxMessages = ValidationUtils.ensureGreaterThanZero(builder.maxMessages, "maxMessages");
        this.store = ValidationUtils.ensureNotNull(builder.store, "store");
    }

    public Object id() {

        return this.id;
    }

    public void add(ChatMessage message) {

        if (message instanceof SystemMessage) {
            Optional<SystemMessage> systemMessage = findSystemMessage(this.store.getMessages(this.id));
            if (systemMessage.isPresent()) {
                log.trace("Ignoring system message {} because a system message already exists in the memory window",
                        message);
                return;
            }
        }
        this.store.updateMessage(this.id, message);
    }

    private static Optional<SystemMessage> findSystemMessage(List<ChatMessage> messages) {

        return messages.stream().filter((message) -> message instanceof SystemMessage)
                .map((message) -> (SystemMessage) message).findAny();
    }

    public List<ChatMessage> messages() {

        List<ChatMessage> messages = new LinkedList(this.store.getMessages(this.id, maxMessages));
        sanitizeMessages(messages);
        return messages;
    }

    private static void sanitizeMessages(List<ChatMessage> messages) {

        Set<String> validToolExecutionIds = new HashSet<>();
        Iterator<ChatMessage> iterator = messages.iterator();
        Set<String> foundResults = new HashSet<>();

        // First pass: Collect tool execution requests and track their IDs
        while (iterator.hasNext()) {
            ChatMessage message = iterator.next();
            if (message instanceof AiMessage aiMessage && aiMessage.hasToolExecutionRequests()) {
                validToolExecutionIds.addAll(
                        aiMessage.toolExecutionRequests().stream().map(ToolExecutionRequest::id).toList());
            } else if (message instanceof ToolExecutionResultMessage resultMessage) {
                if (validToolExecutionIds.contains(resultMessage.id())) {
                    validToolExecutionIds.remove(resultMessage.id());
                    foundResults.add(resultMessage.id());
                } else {
                    iterator.remove(); // Remove result messages without corresponding requests
                }
            }
        }

        // Second pass: Remove tool execution requests without corresponding results
        iterator = messages.iterator();
        while (iterator.hasNext()) {
            ChatMessage message = iterator.next();
            if (message instanceof AiMessage aiMessage && aiMessage.hasToolExecutionRequests()) {
                if (!foundResults.containsAll(
                        aiMessage.toolExecutionRequests().stream().map(ToolExecutionRequest::id).toList())) {
                    iterator.remove();
                }
            }
        }
    }

    public void clear() {

        this.store.deleteMessages(this.id);
    }

    public static Builder builder() {

        return new MessageWindowChatMemoryWithDatabase.Builder();
    }

    public static class Builder {

        private Object id = "default";
        private Integer maxMessages;
        private DatabaseChatMemoryStore store;

        public Builder() {

        }

        public Builder id(Object id) {

            this.id = id;
            return this;
        }

        public Builder maxMessages(Integer maxMessages) {

            this.maxMessages = maxMessages;
            return this;
        }

        public Builder chatMemoryStore(DatabaseChatMemoryStore store) {

            this.store = store;
            return this;
        }

        public MessageWindowChatMemoryWithDatabase build() {

            return new MessageWindowChatMemoryWithDatabase(this);
        }
    }
}
