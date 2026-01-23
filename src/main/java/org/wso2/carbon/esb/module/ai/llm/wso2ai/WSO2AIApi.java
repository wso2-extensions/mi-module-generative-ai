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

import dev.langchain4j.model.anthropic.internal.api.AnthropicCreateMessageRequest;
import dev.langchain4j.model.anthropic.internal.api.AnthropicCreateMessageResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Retrofit API interface for WSO2 AI proxy service.
 * This interface uses Authorization Bearer token instead of x-api-key
 * to authenticate with the Anthropic proxy.
 */
public interface WSO2AIApi {

    @POST("messages")
    @Headers({"content-type: application/json"})
    Call<AnthropicCreateMessageResponse> createMessage(
            @Header("Authorization") String authorization,
            @Header("anthropic-version") String version,
            @Header("User-Agent") String userAgent,
            @Header("Connection") String connection,
            @Body AnthropicCreateMessageRequest request);
}
