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

package org.wso2.carbon.esb.module.ai.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.wso2.carbon.esb.module.ai.memory.MessageWindowChatMemoryWithDatabase;
import org.wso2.carbon.esb.module.ai.memory.store.DatabaseChatMemoryStore;
import org.wso2.carbon.esb.module.ai.memory.store.MemoryStoreHandler;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

public class Utils {

    protected static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Metadata.class, new MetadataSerializer())
            .registerTypeAdapter(Metadata.class, new MetadataDeserializer())
            .registerTypeAdapter(Embedding.class, new EmbeddingSerializer())
            .registerTypeAdapter(Embedding.class, new EmbeddingDeserializer())
            .create();

    public static <T> T fromJson(String json, Type type) {

        return gson.fromJson(json, type);
    }

    public static String toJson(Object object) {

        return gson.toJson(object);
    }

    /**
     * Returns the chat memory for the current chat session ID
     *
     * @param sessionId          Current chat session ID
     * @param memoryConfigKey Memory configuration key
     * @param maxChatHistory  Maximum chat history
     * @return Chat memory
     */
    public static ChatMemory getChatMemory(String sessionId, String memoryConfigKey, int maxChatHistory) {

        if (StringUtils.isEmpty(memoryConfigKey)) {
            return MessageWindowChatMemory.builder().id(sessionId).chatMemoryStore(new InMemoryChatMemoryStore())
                    .maxMessages(maxChatHistory).build();
        }
        ChatMemory chatMemory;
        ChatMemoryStore chatMemoryStore = MemoryStoreHandler.getMemoryStoreHandler().getMemoryStore(memoryConfigKey);
        if (chatMemoryStore instanceof DatabaseChatMemoryStore) {
            chatMemory = MessageWindowChatMemoryWithDatabase.builder().id(sessionId)
                    .chatMemoryStore((DatabaseChatMemoryStore) chatMemoryStore).maxMessages(maxChatHistory).build();
        } else {
            chatMemory = MessageWindowChatMemory.builder().id(sessionId).chatMemoryStore(chatMemoryStore)
                    .maxMessages(maxChatHistory).build();
        }
        return chatMemory;
    }

    public static UserMessage buildUserMessage(String parsedPrompt, String attachments) {

        List<Content> contents = new ArrayList<>();
        if (StringUtils.isNotEmpty(parsedPrompt)) {
            contents.add(TextContent.from(parsedPrompt));
        }
        if (attachments == null || attachments.trim().isEmpty()) {
            return UserMessage.from(contents);
        }
        JsonElement attachmentsElement = gson.fromJson(attachments, JsonElement.class);
        // support both JSON array of attachments and single JSON object
        if (attachmentsElement.isJsonArray()) {
            for (JsonElement attachmentJson : attachmentsElement.getAsJsonArray()) {
                if (attachmentJson.isJsonObject() && attachmentJson.getAsJsonObject().has("type")) {
                    Optional<List<Content>> contentOptional = deserialize(attachmentJson);
                    contentOptional.ifPresent(contents::addAll);
                }
            }
        } else if (attachmentsElement.isJsonObject()) {
            JsonElement attachmentJson = attachmentsElement;
            if (attachmentJson.getAsJsonObject().has("type")) {
                Optional<List<Content>> contentOptional = deserialize(attachmentJson);
                contentOptional.ifPresent(contents::addAll);
            }
        }
        return UserMessage.from(contents);
    }

    private static Optional<List<Content>> deserialize(JsonElement contentJsonElement) {

        try {
            // determine content type
            JsonObject contentObj = contentJsonElement.getAsJsonObject();
            JsonElement typeElem = contentObj.get("type");
            if (typeElem == null || !typeElem.isJsonPrimitive()) {
                return Optional.empty();
            }
            String mimeType = typeElem.getAsString();
            JsonElement contentElem = contentObj.get("content");
            String data = (contentElem != null && contentElem.isJsonPrimitive())
                    ? contentElem.getAsString() : null;
            // handle PDF by MIME type
            if ("application/pdf".equalsIgnoreCase(mimeType)) {
                return buildPdfContent(data);
            }
            if (mimeType.toLowerCase().startsWith("text/")) {
                return Optional.of((List.of(TextContent.from(data))));
            }
            // handle images by MIME prefix
            if (mimeType.toLowerCase().startsWith("image/")) {
                return Optional.of((List.of(ImageContent.from(data, mimeType))));
            }
        } catch (IllegalArgumentException | JsonSyntaxException e) {
            // ignore
        }
        return Optional.empty();
    }

    /**
     * Parses the PDF content from the given base64 encoded string and returns a list of Content objects.
     *
     * @param data Base64 encoded PDF content
     * @return List of {@link Content} objects
     */
    private static Optional<List<Content>> buildPdfContent(String data) {

        if (data == null || data.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            byte[] pdfBytes = Base64.getDecoder().decode(data);
            try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
                List<Content> contents = new ArrayList<>();
                PDFTextStripper textStripper = new PDFTextStripper();
                int numPages = document.getNumberOfPages();
                for (int i = 0; i < numPages; i++) {
                    int pageNumber = i + 1;
                    textStripper.setStartPage(pageNumber);
                    textStripper.setEndPage(pageNumber);
                    String text = textStripper.getText(document).trim();
                    if (!text.isEmpty()) {
                        contents.add(TextContent.from("Parsed Text for Page " + pageNumber + ":\n" + text));
                    }
                    PDPage page = document.getPage(i);
                    PDResources resources = page.getResources();
                    processResources(resources, pageNumber, contents);
                }
                return Optional.of(contents);
            }
        } catch (IllegalArgumentException | IOException e) {
            // ignore or log if necessary
        }
        return Optional.empty();
    }

    /**
     * Recursively extracts image XObjects from the PDF resources and adds them as ImageContent.
     */
    private static void processResources(PDResources resources, int pageNumber, List<Content> contents) {

        if (resources == null) {
            return;
        }
        for (COSName name : resources.getXObjectNames()) {
            try {
                PDXObject xObject = resources.getXObject(name);
                if (xObject instanceof PDImageXObject) {
                    PDImageXObject image = (PDImageXObject) xObject;
                    BufferedImage buffered = image.getImage();
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    // encode image as PNG
                    ImageIO.write(buffered, "png", byteArrayOutputStream);
                    String base64 = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
                    contents.add(ImageContent.from(base64, "image/png"));
                } else if (xObject instanceof PDFormXObject) {
                    // recurse into form XObject
                    PDFormXObject form = (PDFormXObject) xObject;
                    processResources(form.getResources(), pageNumber, contents);
                }
            } catch (IOException e) {
                // skip this resource on error
            }
        }
    }
}
