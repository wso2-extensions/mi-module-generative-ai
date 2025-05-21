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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.VideoContent;

import java.lang.reflect.Type;

public class GsonChatMessageAdapter implements JsonDeserializer<ChatMessage>, JsonSerializer<ChatMessage> {

    private static final Gson
            GSON = (new GsonBuilder()).registerTypeAdapter(Content.class, new GsonContentAdapter()).registerTypeAdapter(
            TextContent.class, new GsonContentAdapter()).registerTypeAdapter(
            ImageContent.class, new GsonContentAdapter()).registerTypeAdapter(
            AudioContent.class, new GsonContentAdapter()).registerTypeAdapter(
            VideoContent.class, new GsonContentAdapter()).registerTypeAdapter(
            PdfFileContent.class, new GsonContentAdapter()).create();
    private static final String CHAT_MESSAGE_TYPE = "type";

    public JsonElement serialize(ChatMessage chatMessage, Type ignored, JsonSerializationContext context) {

        JsonObject messageJsonObject = GSON.toJsonTree(chatMessage).getAsJsonObject();
        messageJsonObject.addProperty(CHAT_MESSAGE_TYPE, chatMessage.type().toString());
        return messageJsonObject;
    }

    public ChatMessage deserialize(JsonElement messageJsonElement, Type ignored, JsonDeserializationContext context) {

        String chatMessageTypeString = messageJsonElement.getAsJsonObject().get(CHAT_MESSAGE_TYPE).getAsString();
        ChatMessageType chatMessageType = ChatMessageType.valueOf(chatMessageTypeString);
        ChatMessage chatMessage = GSON.fromJson(messageJsonElement, chatMessageType.messageClass());
        if (chatMessage instanceof UserMessage message) {
            if (message.contents() == null) {
                chatMessage = UserMessage.from(messageJsonElement.getAsJsonObject().get("text").getAsString());
            }
        }
        return chatMessage;
    }
}
