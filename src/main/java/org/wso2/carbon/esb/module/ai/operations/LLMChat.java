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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;
import org.wso2.carbon.esb.module.ai.llm.LLMConnectionHandler;

import java.lang.reflect.Type;
import java.util.List;

/**
 * LLM Chat mediator
 * @author Isuru Wijesiri
 */
public class LLMChat extends AbstractAIMediator {

    private static final Gson gson = new Gson();

    // Define the agent interfaces for different output types for the LangChain4j service
    interface StringAgent { String chat(String userMessage); }
    interface IntegerAgent { Integer chat(String userMessage); }
    interface FloatAgent { Float chat(String userMessage); }
    interface BooleanAgent { Boolean chat(String userMessage); }

    private static final String DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant.";

    // Chat configurations
    private String modelName;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Double frequencyPenalty;
    private Integer seed;
    private String system;
    private String output;
    private String outputType;
    private String connectionName;

    @Override
    public void initialize(MessageContext mc) {
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

        system = getMediatorParameter(mc, "system", String.class, false);

        connectionName = getProperty(mc, "connectionName", String.class, false);
    }

    @Override
    public void execute(MessageContext mc) {
        String prompt = getMediatorParameter(mc, "prompt", String.class, false);
        String knowledge = getMediatorParameter(mc, "knowledge", String.class, true);

        List<EmbeddingMatch<TextSegment>> parsedKnowledge = parseAndValidateInput(knowledge);
        if (parsedKnowledge == null) {
            handleException("Invalid input format. Expected a JSON array of Objects.", mc);
            return;
        }

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

    private List<EmbeddingMatch<TextSegment>> parseAndValidateInput(String knowledge) {
        try {
            Type listType = new TypeToken<List<EmbeddingMatch<TextSegment>>>() {}.getType();
            List<EmbeddingMatch<TextSegment>> embeddingMatches = gson.fromJson(knowledge, listType);

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

    private Object getChatResponse(String outputType, String prompt, String knowledge) {
        return switch (outputType.toLowerCase()) {
            case "string" -> getAgent(StringAgent.class, knowledge).chat(prompt);
            case "integer" -> getAgent(IntegerAgent.class, knowledge).chat(prompt);
            case "float" -> getAgent(FloatAgent.class, knowledge).chat(prompt);
            case "boolean" -> getAgent(BooleanAgent.class, knowledge).chat(prompt);
            default -> null;
        };
    }

    private <T> T getAgent(Class<T> agentType, String knowledge) {
        ChatLanguageModel model = LLMConnectionHandler.getChatModel(connectionName, modelName, temperature, maxTokens, topP, frequencyPenalty, seed);
        AiServices<T> service = AiServices
                .builder(agentType)
                .chatLanguageModel(model)
                .systemMessageProvider(chatMemoryId -> system != null ? system : DEFAULT_SYSTEM_PROMPT);
        if (knowledge != null && !knowledge.isEmpty()) {
            return service.contentRetriever(query -> List.of(new Content(knowledge))).build();
        }
        return service.build();
    }
}
