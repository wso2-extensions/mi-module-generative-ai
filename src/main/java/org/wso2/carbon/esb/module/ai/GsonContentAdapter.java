/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.esb.module.ai;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ContentType;

import java.lang.reflect.Type;

public class GsonContentAdapter implements JsonDeserializer<Content>, JsonSerializer<Content> {

    private static final Gson GSON = new Gson();
    private static final String CONTENT_TYPE = "type";

    public JsonElement serialize(Content content, Type ignored, JsonSerializationContext context) {

        JsonObject contentJsonObject = GSON.toJsonTree(content).getAsJsonObject();
        contentJsonObject.addProperty(CONTENT_TYPE, content.type().toString());
        return contentJsonObject;
    }

    public Content deserialize(JsonElement contentJsonElement, Type ignored, JsonDeserializationContext context) {

        String contentTypeString = contentJsonElement.getAsJsonObject().get(CONTENT_TYPE).getAsString();
        ContentType contentType = ContentType.valueOf(contentTypeString);
        return GSON.fromJson(contentJsonElement, contentType.getContentClass());
    }
}
