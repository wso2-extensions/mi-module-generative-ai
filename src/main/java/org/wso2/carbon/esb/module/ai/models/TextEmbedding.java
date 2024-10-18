package org.wso2.carbon.esb.module.ai.models;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.langchain4j.data.document.Metadata;

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
        jsonObject.add("embedding", new Gson().toJsonTree(embedding));
        jsonObject.add("metadata", new Gson().toJsonTree(metadata.toMap()));
        return jsonObject;
    }
}
