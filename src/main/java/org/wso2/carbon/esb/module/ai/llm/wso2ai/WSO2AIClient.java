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

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.model.anthropic.internal.api.AnthropicCreateMessageRequest;
import dev.langchain4j.model.anthropic.internal.api.AnthropicCreateMessageResponse;
import dev.langchain4j.model.anthropic.internal.client.AnthropicHttpException;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.time.Duration;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

/**
 * Custom client for WSO2 AI proxy service.
 * This client uses Authorization Bearer token authentication
 * instead of the standard Anthropic x-api-key header.
 */
public class WSO2AIClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(INDENT_OUTPUT);
    private static final String USER_AGENT = "MI-AI-Connector";
    private static final String CONNECTION = "keep-alive";
    private static final String DEFAULT_VERSION = "2023-06-01";

    private final WSO2AIApi wso2AIApi;
    private final String accessToken;
    private final String version;

    private WSO2AIClient(Builder builder) {
        this.accessToken = builder.accessToken;
        this.version = builder.version != null ? builder.version : DEFAULT_VERSION;

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .callTimeout(builder.timeout)
                .connectTimeout(builder.timeout)
                .readTimeout(builder.timeout)
                .writeTimeout(builder.timeout)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Utils.ensureTrailingForwardSlash(builder.baseUrl))
                .client(okHttpClient)
                .addConverterFactory(JacksonConverterFactory.create(OBJECT_MAPPER))
                .build();

        this.wso2AIApi = retrofit.create(WSO2AIApi.class);
    }

    public AnthropicCreateMessageResponse createMessage(AnthropicCreateMessageRequest request) {
        try {
            String authHeader = "Bearer " + accessToken;
            retrofit2.Response<AnthropicCreateMessageResponse> response =
                    wso2AIApi.createMessage(authHeader, version, USER_AGENT, CONNECTION, request).execute();

            if (response.isSuccessful()) {
                return response.body();
            } else {
                try (ResponseBody errorBody = response.errorBody()) {
                    if (errorBody != null) {
                        throw new AnthropicHttpException(response.code(), errorBody.string());
                    }
                }
                throw new AnthropicHttpException(response.code(), null);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String baseUrl;
        private String accessToken;
        private String version;
        private Duration timeout = Duration.ofSeconds(60);

        public Builder baseUrl(String baseUrl) {
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("baseUrl cannot be null or empty");
            }
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder accessToken(String accessToken) {
            if (accessToken == null || accessToken.trim().isEmpty()) {
                throw new IllegalArgumentException("accessToken cannot be null or empty");
            }
            this.accessToken = accessToken;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder timeout(Duration timeout) {
            if (timeout != null) {
                this.timeout = timeout;
            }
            return this;
        }

        public WSO2AIClient build() {
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("baseUrl must be set");
            }
            if (accessToken == null || accessToken.trim().isEmpty()) {
                throw new IllegalArgumentException("accessToken must be set");
            }
            return new WSO2AIClient(this);
        }
    }
}
