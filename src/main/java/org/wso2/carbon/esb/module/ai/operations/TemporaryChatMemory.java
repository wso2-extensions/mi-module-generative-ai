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
