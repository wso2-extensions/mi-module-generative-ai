/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.esb.module.ai.llm.wso2ai;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.anthropic.internal.api.AnthropicCreateMessageRequest;
import dev.langchain4j.model.anthropic.internal.api.AnthropicCreateMessageResponse;
import dev.langchain4j.model.anthropic.internal.api.AnthropicTextContent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.time.Duration;
import java.util.List;

import static dev.langchain4j.internal.RetryUtils.withRetry;
import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.model.anthropic.internal.api.AnthropicCacheType.NO_CACHE;
import static dev.langchain4j.model.anthropic.internal.mapper.AnthropicMapper.toAiMessage;
import static dev.langchain4j.model.anthropic.internal.mapper.AnthropicMapper.toAnthropicMessages;
import static dev.langchain4j.model.anthropic.internal.mapper.AnthropicMapper.toAnthropicSystemPrompt;
import static dev.langchain4j.model.anthropic.internal.mapper.AnthropicMapper.toAnthropicTools;
import static dev.langchain4j.model.anthropic.internal.mapper.AnthropicMapper.toFinishReason;
import static dev.langchain4j.model.anthropic.internal.mapper.AnthropicMapper.toTokenUsage;
import static dev.langchain4j.model.anthropic.internal.sanitizer.MessageSanitizer.sanitizeMessages;

/**
 * ChatLanguageModel implementation for WSO2 AI proxy service.
 * This model connects to an Anthropic API through a WSO2 proxy that uses
 * Bearer token authentication instead of the standard Anthropic x-api-key.
 */
public class WSO2AIChatModel implements ChatLanguageModel {

    private final WSO2AIClient client;
    private final String modelName;
    private final Double temperature;
    private final Double topP;
    private final Integer topK;
    private final int maxTokens;
    private final List<String> stopSequences;
    private final int maxRetries;

    private WSO2AIChatModel(Builder builder) {
        this.client = WSO2AIClient.builder()
                .baseUrl(builder.baseUrl)
                .accessToken(builder.accessToken)
                .version(builder.version)
                .timeout(getOrDefault(builder.timeout, Duration.ofSeconds(60)))
                .build();
        this.modelName = builder.modelName;
        this.temperature = builder.temperature;
        this.topP = builder.topP;
        this.topK = builder.topK;
        this.maxTokens = getOrDefault(builder.maxTokens, 1024);
        this.stopSequences = builder.stopSequences;
        this.maxRetries = getOrDefault(builder.maxRetries, 3);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        return generate(messages, (List<ToolSpecification>) null);
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
        List<ChatMessage> sanitizedMessages = sanitizeMessages(messages);
        List<AnthropicTextContent> systemPrompt = toAnthropicSystemPrompt(messages, NO_CACHE);

        AnthropicCreateMessageRequest request = AnthropicCreateMessageRequest.builder()
                .model(modelName)
                .messages(toAnthropicMessages(sanitizedMessages))
                .system(systemPrompt)
                .maxTokens(maxTokens)
                .stopSequences(stopSequences)
                .stream(false)
                .temperature(temperature)
                .topP(topP)
                .topK(topK)
                .tools(toAnthropicTools(toolSpecifications, NO_CACHE))
                .build();

        AnthropicCreateMessageResponse response = withRetry(() -> client.createMessage(request), maxRetries);

        return Response.from(
                toAiMessage(response.content),
                toTokenUsage(response.usage),
                toFinishReason(response.stopReason)
        );
    }

    public static class Builder {
        private String baseUrl;
        private String accessToken;
        private String version;
        private String modelName;
        private Double temperature;
        private Double topP;
        private Integer topK;
        private Integer maxTokens;
        private List<String> stopSequences;
        private Duration timeout;
        private Integer maxRetries;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder topK(Integer topK) {
            this.topK = topK;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder stopSequences(List<String> stopSequences) {
            this.stopSequences = stopSequences;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public WSO2AIChatModel build() {
            return new WSO2AIChatModel(this);
        }
    }
}
