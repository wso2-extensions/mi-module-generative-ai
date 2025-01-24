package org.wso2.carbon.esb.module.ai.utils;

import com.google.gson.*;
import dev.langchain4j.data.embedding.Embedding;

import java.lang.reflect.Type;

class EmbeddingDeserializer implements JsonDeserializer<Embedding> {
    @Override
    public Embedding deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonArray jsonArray = json.getAsJsonArray();
        float[] vector = new float[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); i++) {
            vector[i] = jsonArray.get(i).getAsFloat();
        }
        return new Embedding(vector);
    }
}
