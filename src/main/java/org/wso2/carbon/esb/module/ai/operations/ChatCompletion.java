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
import dev.langchain4j.service.AiServices;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;

import java.util.concurrent.ConcurrentHashMap;

interface StringAgent {
    String chat(String userMessage);
}

interface IntegerAgent {
    Integer chat(String userMessage);
}

interface FloatAgent {
    Float chat(String userMessage);
}

interface BooleanAgent {
    Boolean chat(String userMessage);
}

/**
 * Chat completion mediator
 * @author Isuru Wijesiri
 */
public class ChatCompletion extends AbstractAIMediator {

    private static final String TEMPLATE_SYSTEM_PROMPT = "systemPrompt";
    private static final String TEMPLATE_PROMPT = "prompt";
    private static final String TEMPLATE_OUTPUT_NAME = "output";
    private static final String TEMPLATE_OUTPUT_TYPE = "outputType";
    private static final String DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant.";
    private static final String API_KEY = "ai_openai_apiKey";

    private static final String TEMPLATE_MODEL_NAME = "modelName";
    private static final String TEMPLATE_TEMPERATURE = "temperature";
    private static final String TEMPLATE_MAX_TOKENS = "maxTokens";
    private static final String TEMPLATE_TOP_P = "topP";
    private static final String TEMPLATE_FREQUENCY_PENALTY = "frequencyPenalty";
    private static final String TEMPLATE_SEED = "seed";

    // Thread safe cache to store the created agent to avoid creating a new agents for each request
    private static final ConcurrentHashMap<String, Object> agentCache = new ConcurrentHashMap<>();

    // Agent configurations
    private String modelName;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Double frequencyPenalty;
    private Integer seed;
    private String apiKey;
    private String systemPrompt;

    @Override
    public void execute(MessageContext mc) {

        // Load mediator configurations from template
        String systemPromptName = getMediatorParameter(mc, TEMPLATE_SYSTEM_PROMPT, String.class, false);
        String promptName = getMediatorParameter(mc, TEMPLATE_PROMPT, String.class, false);
        String output = getMediatorParameter(mc, TEMPLATE_OUTPUT_NAME, String.class, false);
        String outputType = getMediatorParameter(mc, TEMPLATE_OUTPUT_TYPE, String.class, false);

        // Load LLM agent configurations from template and message context
        modelName = getMediatorParameter(mc, TEMPLATE_MODEL_NAME, String.class, false);
        temperature = getMediatorParameter(mc, TEMPLATE_TEMPERATURE, Double.class, true);
        maxTokens = getMediatorParameter(mc, TEMPLATE_MAX_TOKENS, Integer.class, true);
        topP = getMediatorParameter(mc, TEMPLATE_TOP_P, Double.class, true);
        frequencyPenalty = getMediatorParameter(mc, TEMPLATE_FREQUENCY_PENALTY, Double.class, true);
        seed = getMediatorParameter(mc, TEMPLATE_SEED, Integer.class, true);

        apiKey = getProperty(mc, API_KEY, String.class, false);
        systemPrompt = getProperty(mc, systemPromptName, String.class, false);

        String prompt = getProperty(mc, promptName, String.class, false);

        try {
            Object answer = getChatResponse(outputType, prompt);
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

    private Object getChatResponse(String outputType, String prompt) {
        switch (outputType.toLowerCase()) {
            case "string":
                return getAgent(StringAgent.class).chat(prompt);
            case "integer":
                return getAgent(IntegerAgent.class).chat(prompt);
            case "float":
                return getAgent(FloatAgent.class).chat(prompt);
            case "boolean":
                return getAgent(BooleanAgent.class).chat(prompt);
            default:
                return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getAgent(Class<T> agentType) {
        return (T) agentCache.computeIfAbsent( agentType.getName(), key -> {
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
                    .systemMessageProvider(chatMemoryId -> systemPrompt != null ? systemPrompt : DEFAULT_SYSTEM_PROMPT)
                    .build();
        });
    }
}
