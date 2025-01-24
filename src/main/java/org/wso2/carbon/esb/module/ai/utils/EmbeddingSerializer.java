package org.wso2.carbon.esb.module.ai.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import dev.langchain4j.data.embedding.Embedding;

import java.lang.reflect.Type;

class EmbeddingSerializer implements JsonSerializer<Embedding> {
    @Override
    public JsonElement serialize(Embedding embedding, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(embedding.vector());
    }
}
