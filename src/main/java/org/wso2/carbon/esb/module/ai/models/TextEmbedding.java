package org.wso2.carbon.esb.module.ai.models;

import com.google.gson.*;
import dev.langchain4j.data.document.Metadata;

import java.util.HashMap;
import java.util.Map;

public class TextEmbedding {
    private String text;
    private float[] embedding;
    private final Metadata metadata;

    public TextEmbedding(String text, float[] embedding, Metadata metadata) {
        this.text = text;
        this.embedding = embedding;
        this.metadata = metadata;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public JsonElement serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("text", text);
        JsonArray jsonArray = new JsonArray();
        for (float value : embedding) {
            jsonArray.add(value);
        }
        jsonObject.add("embedding", jsonArray);

        JsonObject metadataObject = new JsonObject();
        for (Map.Entry<String, Object> entry : metadata.toMap().entrySet()) {
            metadataObject.add(entry.getKey(), serializeValue(entry.getValue()));
        }
        jsonObject.add("metadata", metadataObject);

        return jsonObject;
    }

    private JsonElement serializeValue(Object value) {
        if (value instanceof Number) {
            return new JsonPrimitive((Number) value);
        } else if (value instanceof Boolean) {
            return new JsonPrimitive((Boolean) value);
        } else if (value instanceof String) {
            return new JsonPrimitive((String) value);
        } else {
            throw new IllegalArgumentException("Unsupported metadata value type: " + value.getClass());
        }
    }

    public static TextEmbedding deserialize(JsonElement json) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String text = jsonObject.get("text").getAsString();
        JsonArray jsonArray = jsonObject.get("embedding").getAsJsonArray();
        float[] embedding = new float[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); i++) {
            embedding[i] = jsonArray.get(i).getAsFloat();
        }

        JsonObject metadataObject = jsonObject.get("metadata").getAsJsonObject();
        Map<String, Object> metadata = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : metadataObject.entrySet()) {
            metadata.put(entry.getKey(), deserializeValue(entry.getValue()));
        }

        return new TextEmbedding(text, embedding, new Metadata(metadata));
    }

    private static Object deserializeValue(JsonElement jsonElement) {
        if (jsonElement.isJsonPrimitive()) {
            JsonPrimitive primitive = jsonElement.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                return primitive.getAsNumber();
            } else if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            } else if (primitive.isString()) {
                return primitive.getAsString();
            }
        }
        throw new JsonParseException("Unsupported metadata value type: " + jsonElement);
    }
}