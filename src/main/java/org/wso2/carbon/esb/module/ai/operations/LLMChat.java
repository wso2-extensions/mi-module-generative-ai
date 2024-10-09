/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
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

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.AiServices;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LLM Chat mediator
 * @author Isuru Wijesiri
 */
public class LLMChat extends AbstractAIMediator {

    // Define the agent interfaces for different output types for the LangChain4j service
    interface StringAgent { String chat(String userMessage); }
    interface IntegerAgent { Integer chat(String userMessage); }
    interface FloatAgent { Float chat(String userMessage); }
    interface BooleanAgent { Boolean chat(String userMessage); }

    private static final String DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant.";

    // Thread safe cache to store the created agent using a unique ID to avoid creating a new agents for each request
    private static final ConcurrentHashMap<String, Object> agentCache = new ConcurrentHashMap<>();

    // Chat configurations
    private String modelName;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Double frequencyPenalty;
    private Integer seed;
    private String apiKey;
    private String system;
    private String output;
    private String outputType;

    @Override
    public void init(MessageContext mc) {
        // Load mediator configurations from template
        output = getMediatorParameter(mc, "output", String.class, false);
        outputType = getMediatorParameter(mc, "outputType", String.class, false);

        // Load configurations from template and message context
        modelName = getMediatorParameter(mc, "modelName", String.class, false);
        temperature = getMediatorParameter(mc, "temperature", Double.class, true);
        maxTokens = getMediatorParameter(mc, "maxTokens", Integer.class, true);
        topP = getMediatorParameter(mc, "topP", Double.class, true);
        frequencyPenalty = getMediatorParameter(mc, "frequencyPenalty", Double.class, true);
        seed = getMediatorParameter(mc, "seed", Integer.class, true);
        apiKey = getProperty(mc, "ai_openai_apiKey", String.class, false);

        system = getMediatorParameter(mc, "role", String.class, false);
    }

    @Override
    public void execute(MessageContext mc) {
        String prompt = getMediatorParameter(mc, "prompt", String.class, false);
        String knowledge = getMediatorParameter(mc, "knowledgeStore", String.class, true);
        try {
            Object answer = getChatResponse(outputType, prompt, knowledge);
            if (answer != null) {
                mc.setProperty(output, answer);
            } else {
                log.error("Invalid output type");
                handleException("Invalid output type", mc);
            }
        } catch (Exception e) {
            log.error("Error while LLM chat completion", e);
            handleException("Error while LLM chat completion", e, mc);
        }
    }

    private Object getChatResponse(String outputType, String prompt, String knowledge) {
        switch (outputType.toLowerCase()) {
            case "string":
                return getAgent(StringAgent.class, knowledge).chat(prompt);
            case "integer":
                return getAgent(IntegerAgent.class, knowledge).chat(prompt);
            case "float":
                return getAgent(FloatAgent.class, knowledge).chat(prompt);
            case "boolean":
                return getAgent(BooleanAgent.class, knowledge).chat(prompt);
            default:
                return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getAgent(Class<T> agentType, String knowledge) {
        // Unique ID for the agent based on the configurations
        String agentId = String.format("%s-%s-%s-%s-%s-%s-%s-%s",
                agentType.getName(), modelName, temperature, maxTokens, topP, frequencyPenalty, seed,
                hashApiKey(apiKey));

        return (T) agentCache.computeIfAbsent(agentId, key -> {
            // Null values of LLM params will be handled by LangChain4j
            OpenAiChatModel model = OpenAiChatModel.builder()
                    .modelName(modelName)
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .topP(topP)
                    .frequencyPenalty(frequencyPenalty)
                    .seed(seed)
                    .apiKey(apiKey)
                    .build();

            return AiServices
                    .builder(agentType)
                    .chatLanguageModel(model)
                    .systemMessageProvider(chatMemoryId -> system != null ? system : DEFAULT_SYSTEM_PROMPT)
                    .contentRetriever(query -> List.of(new Content(Objects.requireNonNullElse(knowledge, ""))))
                    .build();
        });
    }

    // Hash the API key to avoid storing the actual key in the cache
    private String hashApiKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing API key", e);
        }
    }
}
