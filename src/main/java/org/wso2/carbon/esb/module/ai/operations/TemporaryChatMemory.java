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

package org.wso2.carbon.esb.module.ai.operations;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;

import java.util.List;

public class TemporaryChatMemory implements ChatMemory {

    List<ChatMessage> messages;
    private final Integer maxMessages;

    private TemporaryChatMemory(Builder builder) {
        this.maxMessages = builder.maxMessages;
        this.messages = builder.messages;
        resize();
    }

    @Override
    public Object id() {
        return 0;
    }

    @Override
    public void add(ChatMessage chatMessage) {
        messages.add(chatMessage);
        resize();
    }

    @Override
    public List<ChatMessage> messages() {
        return messages;
    }

    @Override
    public void clear() {
        messages = null;
    }

    private void resize() {
        // resize if mexMessages is set
        if (maxMessages != null && messages.size() > maxMessages) {
            messages = messages.subList(messages.size() - maxMessages, messages.size());
        }
    }

    // Builder pattern
    public static class Builder {
        private Integer maxMessages;
        private List<ChatMessage> messages;

        public Builder maxMessages(Integer maxMessages) {
            this.maxMessages = maxMessages;
            return this;
        }

        public Builder from(List<ChatMessage> messages) {
            this.messages = messages;
            return this;
        }

        public TemporaryChatMemory build() {
            return new TemporaryChatMemory(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
