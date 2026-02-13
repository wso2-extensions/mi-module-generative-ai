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

package org.wso2.carbon.esb.module.ai.llm;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.wso2.carbon.esb.module.ai.Constants;
import org.wso2.carbon.esb.module.ai.connections.ConnectionParams;
import org.wso2.carbon.esb.module.ai.llm.wso2ai.WSO2AIChatModel;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class LLMConnectionHandler {

    private final static ConcurrentHashMap<String, ConnectionParams> connectionMap = new ConcurrentHashMap<>();

    public static void addConnection(String connectionName, ConnectionParams connectionParams) {
        connectionMap.computeIfAbsent(connectionName, k -> connectionParams);
    }

    public static ChatModel getChatModel(
            String connectionName, String modelName, Double temperature, Integer maxTokens,
            Double topP, Double frequencyPenalty, Integer seed) {

        // TODO: Need to pool the models as it is resource intensive to create a new model for each request
        ChatModel chatModel = null;
        ConnectionParams connectionParams = connectionMap.get(connectionName);
        if (connectionParams == null) {
            return null;
        }

        switch (Objects.requireNonNull(connectionParams).getConnectionType()) {
            case Constants.OPEN_AI:
            case Constants.DEEPSEEK:
                // Null values of LLM params will be handled by LangChain4j
                 chatModel =  OpenAiChatModel.builder()
                         .baseUrl(connectionParams.getConnectionProperty(Constants.BASE_URL))
                         .modelName(modelName)
                         .temperature(temperature)
                         .maxTokens(maxTokens)
                         .topP(topP)
                         .frequencyPenalty(frequencyPenalty)
                         .seed(seed)
                         .apiKey(connectionParams.getConnectionProperty(Constants.API_KEY))
                         .build();
                 break;
            case Constants.AZURE_OPEN_AI:
                // Null values of LLM params will be handled by LangChain4j
                chatModel =  AzureOpenAiChatModel.builder()
                        .temperature(temperature)
                        .maxTokens(maxTokens)
                        .topP(topP)
                        .frequencyPenalty(frequencyPenalty)
                        .seed(Long.valueOf(seed))
                        .apiKey(connectionParams.getConnectionProperty(Constants.API_KEY))
                        .deploymentName(connectionParams.getConnectionProperty(Constants.DEPLOYMENT_NAME))
                        .endpoint(connectionParams.getConnectionProperty(Constants.ENDPOINT))
                        .build();
                break;
            case Constants.ANTHROPIC:
                chatModel = AnthropicChatModel.builder()
                        .modelName(modelName)
                        .temperature(temperature)
                        .maxTokens(maxTokens)
                        .topP(topP)
                        .apiKey(connectionParams.getConnectionProperty(Constants.API_KEY))
                        .build();
                break;
            case Constants.MISTRAL_AI:
                chatModel = MistralAiChatModel.builder()
                        .modelName(modelName)
                        .temperature(temperature)
                        .maxTokens(maxTokens)
                        .topP(topP)
                        .apiKey(connectionParams.getConnectionProperty(Constants.API_KEY))
                        .build();
                break;
            case Constants.WSO2_AI:
                WSO2AIChatModel.Builder chatModelBuilder
                        = WSO2AIChatModel.builder()
                        .baseUrl(connectionParams.getConnectionProperty(Constants.SERVICE_URL))
                        .accessToken(connectionParams.getConnectionProperty(Constants.ACCESS_TOKEN))
                        .maxTokens(maxTokens);
                // Can only set one of temperature or topP
                if (temperature != null) {
                    chatModelBuilder.temperature(temperature);
                } else if (topP != null) {
                    chatModelBuilder.topP(topP);
                }
                chatModel = chatModelBuilder.build();
                break;
            default:
                break;
        }
        return chatModel;
    }

    public static EmbeddingModel getEmbeddingModel(String connectionName, String modelName) {
        EmbeddingModel embeddingModel = null;
        ConnectionParams connectionParams = connectionMap.get(connectionName);
        switch (Objects.requireNonNull(connectionParams).getConnectionType()) {
            case "OPEN_AI":
                // Null values of LLM params will be handled by LangChain4j
                embeddingModel = OpenAiEmbeddingModel.builder()
                        .apiKey(connectionParams.getConnectionProperty(Constants.API_KEY))
                        .modelName(modelName)
                        .build();
            default:
                break;
        }
        return embeddingModel;
    }
}
