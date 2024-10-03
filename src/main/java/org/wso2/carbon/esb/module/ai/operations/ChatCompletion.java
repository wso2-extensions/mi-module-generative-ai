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
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;
import org.wso2.carbon.esb.module.ai.rag.KnowledgeStore;

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

    private static final String DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant.";

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
    private String knowledgeStoreName;
    private KnowledgeStore knowledgeStore;

    @Override
    public void execute(MessageContext mc) {

        // Load mediator configurations from template
        String systemPromptName = getMediatorParameter(mc, "systemPrompt", String.class, false);
        String promptName = getMediatorParameter(mc, "prompt", String.class, false);
        String output = getMediatorParameter(mc, "output", String.class, false);
        String outputType = getMediatorParameter(mc, "outputType", String.class, false);

        // Load LLM agent configurations from template and message context
        modelName = getMediatorParameter(mc, "modelName", String.class, false);
        temperature = getMediatorParameter(mc, "temperature", Double.class, true);
        maxTokens = getMediatorParameter(mc, "maxTokens", Integer.class, true);
        topP = getMediatorParameter(mc, "topP", Double.class, true);
        frequencyPenalty = getMediatorParameter(mc, "frequencyPenalty", Double.class, true);
        seed = getMediatorParameter(mc, "seed", Integer.class, true);

        apiKey = getProperty(mc, "ai_openai_apiKey", String.class, false);
        systemPrompt = getProperty(mc, systemPromptName, String.class, false);

        String prompt = getProperty(mc, promptName, String.class, false);

        // RAG configurations
        knowledgeStoreName = getMediatorParameter(mc, "knowledgeStore", String.class, true);
        // Does not need to be thread safe
        if (knowledgeStore == null) {
            knowledgeStore = (KnowledgeStore) getObjetFromMC(mc, "VECTOR_STORE_" + knowledgeStoreName, false);
        }

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
                    .contentRetriever(getContentRetriever(knowledgeStore))
                    .build();
        });
    }

    // TODO: Use advanced retrieval augmentor
    private ContentRetriever getContentRetriever(KnowledgeStore knowledgeStore) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(knowledgeStore.getEmbeddingStore())
                .embeddingModel(knowledgeStore.getEmbeddingModel())
                .maxResults(2) // on each interaction we will retrieve the 2 most relevant segments
                .minScore(0.5) // we want to retrieve segments at least somewhat similar to user query
                .build();
    }
}
