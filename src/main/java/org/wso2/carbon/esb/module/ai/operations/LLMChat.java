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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.apache.commons.lang3.StringUtils;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;
import org.wso2.carbon.esb.module.ai.Constants;
import org.wso2.carbon.esb.module.ai.Errors;
import org.wso2.carbon.esb.module.ai.llm.LLMConnectionHandler;
import org.wso2.carbon.esb.module.ai.utils.Utils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Language model chat operation
 * Inputs:
 * - modelName: Name of the language model
 * - temperature: Sampling temperature
 * - maxTokens: Maximum tokens to generate
 * - topP: Top P value
 * - frequencyPenalty: Frequency penalty
 * - seed: Random seed
 * - system: System message
 * - prompt: User message
 * - knowledge: JSON array of TextSegment objects
 * - history: JSON array of ChatMessage objects
 * - maxHistory: Maximum history size
 * - connectionName: Name of the connection to the LLM
 * Outputs:
 * - Response based on the output type
 */
public class LLMChat extends AbstractAIMediator {

    private static final Gson gson = new Gson();

    // Define the agent interfaces for different output types for the LangChain4j service
    interface StringAgent { Result<String> chat(UserMessage userMessage); }
    interface IntegerAgent { Result<Integer> chat(UserMessage userMessage); }
    interface FloatAgent { Result<Float> chat(UserMessage userMessage); }
    interface BooleanAgent { Result<Boolean> chat(UserMessage userMessage); }

    private static final String DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant.";

    @Override
    public void execute(MessageContext mc, String responseVariable, Boolean overwriteBody) {

        String connectionName = getProperty(mc, Constants.CONNECTION_NAME, String.class, false);

        String prompt = parseInlineExpression(mc, getMediatorParameter(mc, Constants.PROMPT, String.class, false));
        String attachments = parseInlineExpression(mc,
                getMediatorParameter(mc, Constants.ATTACHMENTS, String.class, true));
        UserMessage userMessage = Utils.buildUserMessage(prompt, attachments);

        String modelName = getMediatorParameter(mc, Constants.MODEL_NAME, String.class, false);
        String outputType = getMediatorParameter(mc, Constants.OUTPUT_TYPE, String.class, false);

        // Advanced configurations
        String systemPrompt = getMediatorParameter(mc, Constants.SYSTEM, String.class, true);
        String parsedSystemPrompt = parseInlineExpression(mc, systemPrompt);

        Double temperature = getMediatorParameter(mc, Constants.TEMPERATURE, Double.class, true);
        Integer maxTokens = getMediatorParameter(mc, Constants.MAX_TOKENS, Integer.class, true);
        Double topP = getMediatorParameter(mc, Constants.TOP_P, Double.class, true);
        Double frequencyPenalty = getMediatorParameter(mc, Constants.FREQUENCY_PENALTY, Double.class, true);
        Integer seed = getMediatorParameter(mc, Constants.SEED, Integer.class, true);

        ChatModel model;
        try {
            model = LLMConnectionHandler.getChatModel(connectionName, modelName, temperature, maxTokens, topP, frequencyPenalty, seed);
            if (model == null) {
                handleConnectorException(Errors.LLM_CONNECTION_ERROR, mc);
                return;
            }
        } catch (Exception e) {
            handleConnectorException(Errors.LLM_CONNECTION_ERROR, mc, e);
            return;
        }

        // Additional configurations
        String knowledge = getMediatorParameter(mc, Constants.KNOWLEDGE, String.class, true);
        Integer maxHistory = getMediatorParameter(mc, Constants.MAX_HISTORY, Integer.class, true);

        ContentRetriever knowledgeRetriever = null;
        if (knowledge != null) {
            List<EmbeddingMatch<TextSegment>> parsedKnowledge = parseAndValidateKnowledge(knowledge);
            if (parsedKnowledge == null) {
                handleConnectorException(Errors.INVALID_INPUT_FOR_CHAT_KNOWLEDGE, mc);
                return;
            }

            // Extract text segments from the parsed knowledge and convert to content
            List<Content> knowledgeTexts = parsedKnowledge.stream()
                    .map(match -> Content.from(match.embedded()))
                    .toList();
            knowledgeRetriever = query -> knowledgeTexts;
        }

        Object memoryConfigKeyObj = mc.getProperty(Constants.MEMORY_CONFIG_KEY);
        String memoryConfigKey = memoryConfigKeyObj != null ? memoryConfigKeyObj.toString() : null;
        String sessionId =
                getMediatorParameter(mc, Constants.SESSION_ID, String.class, StringUtils.isEmpty(memoryConfigKey));
        if (StringUtils.isEmpty(sessionId)) {
            sessionId = "default";
        }
        int maxChatHistory = maxHistory != null ? maxHistory : 20;
        ChatMemory chatMemory = Utils.getChatMemory(sessionId, memoryConfigKey, maxChatHistory);

        try {
            Object answer =
                    getChatResponse(model, outputType, userMessage, knowledgeRetriever, chatMemory, parsedSystemPrompt);
            if (answer != null) {
                handleConnectorResponse(mc, responseVariable, overwriteBody, answer, null, null);
            } else {
                handleConnectorException(Errors.INVALID_OUTPUT_TYPE, mc);
            }
        } catch (Exception e) {
            handleConnectorException(Errors.CHAT_COMPLETION_ERROR, mc, e);
        }
    }

    private List<EmbeddingMatch<TextSegment>> parseAndValidateKnowledge(String knowledge) {
        try {
            Type listType = new TypeToken<List<EmbeddingMatch<TextSegment>>>() {}.getType();
            List<EmbeddingMatch<TextSegment>> embeddingMatches = Utils.fromJson(knowledge, listType);

            // Validate the parsed list
            if (embeddingMatches != null) {
                for (EmbeddingMatch<TextSegment> match : embeddingMatches) {
                    if (match.embedding() == null || match.embedded() == null) {
                        return null;
                    }
                }
            }
            return embeddingMatches;
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    private List<ChatMessage> parseAndValidateChatHistory(String chatHistory) {
        try {
            Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
            List<Map<String, String>> rawMessages = gson.fromJson(chatHistory, listType);

            List<ChatMessage> chatMessages = new ArrayList<>();
            for (Map<String, String> rawMessage : rawMessages) {
                String role = rawMessage.get("role");
                String content = rawMessage.get("content");

                if (role == null || content == null) {
                    return null; // Invalid format
                }

                ChatMessage chatMessage;
                switch (role) {
                    case "user":
                        chatMessage = new UserMessage(content);
                        break;
                    case "assistant":
                        chatMessage = new AiMessage(content);
                        break;
                    default:
                        return null; // Invalid role
                }
                chatMessages.add(chatMessage);
            }
            return chatMessages;
        } catch (JsonSyntaxException e) {
            return null; // Invalid JSON format
        }
    }

    private Object getChatResponse(ChatModel model, String outputType, UserMessage userMessage,
                                   ContentRetriever knowledgeRetriever, ChatMemory chatMemory, String systemMessage) {

        Result<?> result = switch (outputType.toLowerCase()) {
            case "string" ->
                    getAgent(model, StringAgent.class, knowledgeRetriever, chatMemory, systemMessage).chat(userMessage);
            case "integer" -> getAgent(model, IntegerAgent.class, knowledgeRetriever, chatMemory, systemMessage).chat(
                    userMessage);
            case "float" ->
                    getAgent(model, FloatAgent.class, knowledgeRetriever, chatMemory, systemMessage).chat(userMessage);
            case "boolean" -> getAgent(model, BooleanAgent.class, knowledgeRetriever, chatMemory, systemMessage).chat(
                    userMessage);
            default -> null;
        };

        if (result == null) {
            return null;
        }

        // Build response object matching the format from version 0.1.8
        return buildResponseObject(result);
    }

    private Map<String, Object> buildResponseObject(Result<?> result) {
        // Use LinkedHashMap to preserve field order matching version 0.1.8
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        
        // Field order: content -> tokenUsage -> finishReason -> toolExecutions
        response.put("content", result.content());
        
        // Add token usage information
        if (result.tokenUsage() != null) {
            Map<String, Object> tokenUsage = new java.util.LinkedHashMap<>();
            tokenUsage.put("cacheCreationInputTokens", 0); // Default values for backward compatibility
            tokenUsage.put("cacheReadInputTokens", 0);
            tokenUsage.put("inputTokenCount", result.tokenUsage().inputTokenCount());
            tokenUsage.put("outputTokenCount", result.tokenUsage().outputTokenCount());
            tokenUsage.put("totalTokenCount", result.tokenUsage().totalTokenCount());
            
            response.put("tokenUsage", tokenUsage);
        }
        
        // Add finish reason
        if (result.finishReason() != null) {
            response.put("finishReason", result.finishReason().toString());
        }
        
        // Add tool executions (empty array for backward compatibility)
        response.put("toolExecutions", result.toolExecutions() != null ? result.toolExecutions() : new ArrayList<>());
        
        return response;
    }

    private <T> T getAgent(ChatModel model, Class<T> agentType, ContentRetriever knowledgeRetriever,
                           ChatMemory chatMemory, String system) {

        AiServices<T> service = AiServices
                .builder(agentType)
                .chatModel(model);
        if (system != null) {
            service = service.systemMessageProvider(chatMemoryId -> system);
        }
        if (knowledgeRetriever != null) {
            service = service.contentRetriever(knowledgeRetriever);
        }
        if (chatMemory != null) {
            service = service.chatMemory(chatMemory);
        }
        return service.build();
    }
}
