package org.wso2.carbon.esb.module.ai.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class TextEmbedding {
    private String text;
    private float[] embedding;

    public TextEmbedding(String text, float[] embedding) {
        this.text = text;
        this.embedding = embedding;
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

    public JsonElement serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("text", text);
        JsonArray jsonArray = new JsonArray();
        for (float value : embedding) {
            jsonArray.add(value);
        }
        jsonObject.add("embedding", jsonArray);
        return jsonObject;
    }

    public static TextEmbedding deserialize(JsonElement json) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String text = jsonObject.get("text").getAsString();
        JsonArray jsonArray = jsonObject.get("embedding").getAsJsonArray();
        float[] embedding = new float[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); i++) {
            embedding[i] = jsonArray.get(i).getAsFloat();
        }
        return new TextEmbedding(text, embedding);
    }
}
