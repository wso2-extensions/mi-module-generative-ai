/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            "Your task is to create a comprehensive yet concise summary of the conversation below. " +
            "This summary will be used to maintain context when the conversation history exceeds memory limits.\n\n" +
            "CRITICAL: Give MORE PROMINENCE to the MOST RECENT messages, as they represent the current context, " +
            "active work, and direction of the conversation. Earlier messages provide background but recent ones " +
            "are crucial for continuation.\n\n" +
            "INSTRUCTIONS:\n" +
            "Before providing your final summary, wrap your analysis in <analysis> tags to organize your thoughts. " +
            "In your analysis section:\n" +
            "- Review the conversation chronologically from oldest to newest\n" +
            "- Identify ALL of the user's explicit requests and intents (prioritize recent messages)\n" +
            "- Note key decisions, outcomes, technical details, and code patterns\n" +
            "- Identify what context is absolutely essential for continuing the work\n" +
            "- Consider what information would be lost if not preserved\n\n" +
            "Then provide your summary within <summary> tags. Your summary should include:\n" +
            "1. PRIMARY FOCUS - Recent Context (last 3-5 exchanges):\n" +
            "   - What is the user currently working on?\n" +
            "   - What are their most recent explicit requests?\n" +
            "   - What is the immediate next step or current task?\n\n" +
            "2. User's Explicit Requests and Intents:\n" +
            "   - All tasks the user has requested (emphasize recent ones)\n" +
            "   - Goals and objectives they want to achieve\n" +
            "   - Any constraints or preferences they've specified\n\n" +
            "3. Key Decisions and Outcomes:\n" +
            "   - Important technical decisions made\n" +
            "   - Completed tasks and their results\n" +
            "   - Any issues encountered and how they were resolved\n\n" +
            "4. Technical Context (if applicable):\n" +
            "   - File names, paths, and code sections discussed\n" +
            "   - Frameworks, libraries, or technologies involved\n" +
            "   - Configuration changes or architectural decisions\n\n" +
            "5. Continuation Context:\n" +
            "   - What work remains to be done?\n" +
            "   - Any pending questions or unclear requirements\n" +
            "   - Dependencies or prerequisites for next steps\n\n" +
            "6. Previous Summary Integration (if present):\n" +
            "   - If the conversation contains a previous summary, integrate its critical information\n" +
            "   - Update outdated information with new developments\n" +
            "   - Preserve essential historical context while emphasizing recent changes\n\n" +
            "EXAMPLE FORMAT:\n" +
            "<analysis>\n" +
            "[Your thought process here - chronological review, identifying patterns, noting important details]\n" +
            "</analysis>\n\n" +
            "<summary>\n" +
            "RECENT CONTEXT: User is currently [describe current work/task from recent messages]. " +
            "They just [recent actions/requests].\n\n" +
            "KEY REQUESTS: [List all user requests with emphasis on recent ones]\n\n" +
            "DECISIONS & OUTCOMES: [Important decisions and completed work]\n\n" +
            "TECHNICAL DETAILS: [Files, code, configurations - if applicable]\n\n" +
            "NEXT STEPS: [What needs to happen next based on the conversation]\n" +
            "</summary>\n\n" +
            "Keep the summary informative but concise. Aim for clarity and completeness while avoiding unnecessary verbosity.\n\n" +
            "Conversation:\n";

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
     * Extracts content within <summary> tags from the LLM response.
     * Uses regex to find and extract only the summary portion, discarding the analysis.
     * If no summary tags are found, returns the full response as a fallback.
     * 
     * @param fullResponse the complete response from the LLM (may include <analysis> and <summary> tags)
     * @return the extracted summary content, or the full response if tags are not found
     */
    private String extractSummary(String fullResponse) {
        if (fullResponse == null || fullResponse.isEmpty()) {
            return "";
        }
        
        Pattern pattern = Pattern.compile("<summary>\\s*(.+?)\\s*</summary>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(fullResponse);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // Fallback: if no summary tags found, return the full response
        log.warn("No <summary> tags found in summarization response. Using full response.");
        return fullResponse.trim();
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
            String fullResponse = response.aiMessage().text();
            
            // Extract only the content within <summary> tags, discarding <analysis>
            String summary = extractSummary(fullResponse);
            
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
