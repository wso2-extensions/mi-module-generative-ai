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

import dev.langchain4j.exception.HttpException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.HttpClientBuilderLoader;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom HttpClientBuilder for WSO2 AI proxy authentication. Produces an HttpClient that replaces the standard
 * Anthropic {@code x-api-key} header with {@code Authorization: Bearer <token>} and injects the required WSO2 AI proxy
 * routing headers ({@code x-product}, {@code x-usage-context}, {@code x-metadata}), enabling use of AnthropicChatModel
 * against a WSO2-hosted proxy without a direct API key.
 */
public class BearerTokenHttpClientBuilder implements HttpClientBuilder {

    private final String accessToken;
    private Duration connectTimeout;
    private Duration readTimeout;

    public BearerTokenHttpClientBuilder(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public Duration connectTimeout() {
        return connectTimeout;
    }

    @Override
    public HttpClientBuilder connectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    @Override
    public Duration readTimeout() {
        return readTimeout;
    }

    @Override
    public HttpClientBuilder readTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    @Override
    public HttpClient build() {
        HttpClientBuilder inner = HttpClientBuilderLoader.loadHttpClientBuilder();
        if (connectTimeout != null) {
            inner.connectTimeout(connectTimeout);
        }
        if (readTimeout != null) {
            inner.readTimeout(readTimeout);
        }
        return new BearerTokenHttpClient(inner.build(), accessToken);
    }

    /**
     * HttpClient wrapper that swaps {@code x-api-key} for {@code Authorization: Bearer} and injects required WSO2 AI
     * proxy headers.
     */
    private record BearerTokenHttpClient(HttpClient delegate, String accessToken) implements HttpClient {

        @Override
        public SuccessfulHttpResponse execute(HttpRequest request) throws HttpException {
            return delegate.execute(withBearerAuth(request));
        }

        @Override
        public void execute(HttpRequest request, ServerSentEventParser parser,
                            ServerSentEventListener listener) {
            delegate.execute(withBearerAuth(request), parser, listener);
        }

        private HttpRequest withBearerAuth(HttpRequest original) {
            Map<String, List<String>> headers = new LinkedHashMap<>(original.headers());

            headers.remove("x-api-key");
            headers.put("Authorization", new ArrayList<>(List.of("Bearer " + accessToken)));

            // Required headers for WSO2 AI proxy
            headers.put("User-Agent", new ArrayList<>(List.of("MI-VSCode-Plugin")));

            return HttpRequest.builder()
                    .method(original.method())
                    .url(original.url())
                    .headers(headers)
                    .body(original.body())
                    .build();
        }
    }
}
