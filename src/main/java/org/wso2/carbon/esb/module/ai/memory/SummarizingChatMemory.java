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

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A ChatMemory wrapper that automatically summarizes chat history when it exceeds the maximum limit.
 * When capacity is reached, all existing messages are summarized before adding the new incoming message.
 */
public class SummarizingChatMemory implements ChatMemory {

    protected Log log = LogFactory.getLog(this.getClass());
    
    private final ChatMemory underlyingMemory;
    private final ChatModel summarizationModel;
    private final int maxMessages;
    private final Object memoryId;
    
    private static final String SUMMARIZATION_PROMPT = 
            "Please provide a concise summary of the following conversation history. " +
            "Focus on key points, decisions made, and important context that would be needed to continue the conversation. " +
            "If the conversation includes multiple exchanges, try to capture the overall flow and main topics discussed. " +
            "Additionally, if the conversation contains a previous summary, make sure not to loose important details that were in the original messages. " +
            "Keep the summary brief but informative.\n\nConversation:\n";

    public SummarizingChatMemory(ChatMemory underlyingMemory, 
                                  ChatModel summarizationModel, 
                                  int maxMessages,
                                  Object memoryId) {
        this.underlyingMemory = underlyingMemory;
        this.summarizationModel = summarizationModel;
        this.maxMessages = maxMessages;
        this.memoryId = memoryId;
    }

    @Override
    public Object id() {
        return underlyingMemory.id();
    }

    @Override
    public void add(ChatMessage message) {
        // Check if we need to summarize before adding the new message
        List<ChatMessage> currentMessages = underlyingMemory.messages();
        
        // Trigger summarization if we're at the limit
        if (currentMessages.size() >= maxMessages) {
            performSummarization();
        }
        
        underlyingMemory.add(message);
    }

    @Override
    public List<ChatMessage> messages() {
        return underlyingMemory.messages();
    }

    @Override
    public void clear() {
        underlyingMemory.clear();
    }

    /**
     * Performs summarization of the chat history.
     * Summarizes messages from position 1 to maxMessages (excluding system message at position 0)
     * and replaces them with a single summary message at position 1.
     */
    private void performSummarization() {
        try {
            List<ChatMessage> allMessages = underlyingMemory.messages();
            
            if (allMessages.size() <= 2) {
                // Not enough messages to summarize
                return;
            }
            
            // Extract system message (position 0)
            SystemMessage systemMessage = null;
            int startIndex = 0;
            if (!allMessages.isEmpty() && allMessages.get(0) instanceof SystemMessage) {
                systemMessage = (SystemMessage) allMessages.get(0);
                startIndex = 1;
            }
            
            // The new incoming message will be added after summarization
            List<ChatMessage> messagesToSummarize = new ArrayList<>();
            for (int i = startIndex; i < allMessages.size(); i++) {
                messagesToSummarize.add(allMessages.get(i));
            }
            
            if (messagesToSummarize.isEmpty()) {
                return;
            }
            
            // Build conversation text for summarization
            StringBuilder conversationText = new StringBuilder();
            for (ChatMessage msg : messagesToSummarize) {
                if (msg instanceof UserMessage) {
                    UserMessage userMsg = (UserMessage) msg;
                    String text = userMsg.singleText();
                    conversationText.append("User: ").append(text).append("\n");
                } else if (msg instanceof AiMessage) {
                    AiMessage aiMsg = (AiMessage) msg;
                    String text = aiMsg.text();
                    if (text != null) {
                        conversationText.append("Assistant: ").append(text).append("\n");
                    }
                } else if (msg instanceof SystemMessage) {
                    // Skip additional system messages
                    continue;
                }
            }
            
            // Generate summary using the summarization model
            String summaryPrompt = SUMMARIZATION_PROMPT + conversationText.toString();
            UserMessage summarizationRequest = new UserMessage(summaryPrompt);
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(summarizationRequest)
                    .build();
            ChatResponse response = summarizationModel.chat(chatRequest);
            String summary = response.aiMessage().text();
            
            underlyingMemory.clear();
            
            // Rebuild memory with: system message (if exists) + summary as user message
            if (systemMessage != null) {
                underlyingMemory.add(systemMessage);
            }
            
            // Add summary as a user message at position 1
            UserMessage summaryMessage = new UserMessage("Previous conversation summary: " + summary);
            underlyingMemory.add(summaryMessage);
        } catch (Exception e) {
            log.error("Failed to perform chat history summarization. Continuing with trim behavior.", e);
            // If summarization fails, let the underlying memory handle overflow (trim behavior)
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ChatMemory underlyingMemory;
        private ChatModel summarizationModel;
        private int maxMessages = 10;
        private Object memoryId;

        public Builder underlyingMemory(ChatMemory memory) {
            this.underlyingMemory = memory;
            return this;
        }

        public Builder summarizationModel(ChatModel model) {
            this.summarizationModel = model;
            return this;
        }

        public Builder maxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
            return this;
        }

        public Builder memoryId(Object memoryId) {
            this.memoryId = memoryId;
            return this;
        }

        public SummarizingChatMemory build() {
            if (underlyingMemory == null) {
                throw new IllegalArgumentException("Underlying memory must be provided");
            }
            if (summarizationModel == null) {
                throw new IllegalArgumentException("Summarization model must be provided");
            }
            return new SummarizingChatMemory(underlyingMemory, summarizationModel, maxMessages, memoryId);
        }
    }
}
