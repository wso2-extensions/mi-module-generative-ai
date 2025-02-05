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
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;
import org.wso2.carbon.esb.module.ai.llm.LLMConnectionHandler;
import org.wso2.carbon.esb.module.ai.utils.Utils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Language model chat operation
 *
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
 * - responseVariable: Variable name to store the output
 * - responseType: Output type (String, Integer, Float, Boolean)
 * - connectionName: Name of the connection to the LLM
 *
 * Outputs:
 * - Response based on the output type
 */
public class LLMChat extends AbstractAIMediator {

    private static final Gson gson = new Gson();

    // Define the agent interfaces for different output types for the LangChain4j service
    interface StringAgent { Result<String> chat(String userMessage); }
    interface IntegerAgent { Result<Integer> chat(String userMessage); }
    interface FloatAgent { Result<Float> chat(String userMessage); }
    interface BooleanAgent { Result<Boolean> chat(String userMessage); }

    private static final String DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant.";

    // Chat configurations
    private String modelName;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Double frequencyPenalty;
    private Integer seed;
    private String system;
    private String responseVariable;
    private String outputType;
    private String connectionName;

    @Override
    public void initialize(MessageContext mc) {
        // Load mediator configurations from template
        responseVariable = getMediatorParameter(mc, "responseVariable", String.class, false);
        outputType = getMediatorParameter(mc, "outputType", String.class, false);

        // Load configurations from template and message context
        modelName = getMediatorParameter(mc, "modelName", String.class, false);
        temperature = getMediatorParameter(mc, "temperature", Double.class, true);
        maxTokens = getMediatorParameter(mc, "maxTokens", Integer.class, true);
        topP = getMediatorParameter(mc, "topP", Double.class, true);
        frequencyPenalty = getMediatorParameter(mc, "frequencyPenalty", Double.class, true);
        seed = getMediatorParameter(mc, "seed", Integer.class, true);

        system = getMediatorParameter(mc, "system", String.class, false);

        connectionName = getProperty(mc, "connectionName", String.class, false);
    }

    @Override
    public void execute(MessageContext mc) {
        String prompt = getMediatorParameter(mc, "prompt", String.class, false);
        String knowledge = getMediatorParameter(mc, "knowledge", String.class, true);
        String chatHistory = getMediatorParameter(mc, "history", String.class, true);
        Integer maxHistory = getMediatorParameter(mc, "maxHistory", Integer.class, true);

        List<Content> knowledgeTexts;
        if (knowledge != null) {
            List<EmbeddingMatch<TextSegment>> parsedKnowledge = parseAndValidateKnowledge(knowledge);
            if (parsedKnowledge == null) {
                handleException("Invalid input format. Expected a JSON array of Objects.", mc);
                return;
            }

            // Extract text segments from the parsed knowledge and convert to content
            knowledgeTexts = parsedKnowledge.stream()
                    .map(match -> new Content(match.embedded()))
                    .toList();
        } else {
            knowledgeTexts = null;
        }
        ContentRetriever knowledgeRetriever = query -> knowledgeTexts;

        List<ChatMessage> chatMessages = null;
        if (chatHistory != null) {
            chatMessages = parseAndValidateChatHistory(chatHistory);
            if (chatMessages == null) {
                handleException("Invalid chat history format. Expected a JSON array of ChatMessage objects. Use OpenAI format", mc);
                return;
            }
        }
        ChatMemory chatMemory = TemporaryChatMemory.builder()
                .from(chatMessages)
                .maxMessages(maxHistory)
                .build();

        try {
            Object answer = getChatResponse(outputType, prompt, knowledgeRetriever, chatMemory);
            if (answer != null) {
                handleResponse(mc, responseVariable, answer, null, null);
            } else {
                log.error("Invalid output type");
                handleException("Invalid output type", mc);
            }
        } catch (Exception e) {
            log.error("Error while LLM chat completion", e);
            handleException("Error while LLM chat completion", e, mc);
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

    private Object getChatResponse(String outputType, String prompt, ContentRetriever knowledge, ChatMemory chatMemory) {
        return switch (outputType.toLowerCase()) {
            case "string" -> getAgent(StringAgent.class, knowledge, chatMemory).chat(prompt);
            case "integer" -> getAgent(IntegerAgent.class, knowledge, chatMemory).chat(prompt);
            case "float" -> getAgent(FloatAgent.class, knowledge, chatMemory).chat(prompt);
            case "boolean" -> getAgent(BooleanAgent.class, knowledge, chatMemory).chat(prompt);
            default -> null;
        };
    }

    private <T> T getAgent(Class<T> agentType, ContentRetriever knowledge, ChatMemory chatMemory) {
        ChatLanguageModel model = LLMConnectionHandler.getChatModel(connectionName, modelName, temperature, maxTokens, topP, frequencyPenalty, seed);
        AiServices<T> service = AiServices
                .builder(agentType)
                .chatLanguageModel(model)
                .systemMessageProvider(chatMemoryId -> system != null ? system : DEFAULT_SYSTEM_PROMPT)
                .chatMemory(chatMemory);
        if (knowledge != null) {
            return service.contentRetriever(knowledge).build();
        }
        return service.build();
    }
}
