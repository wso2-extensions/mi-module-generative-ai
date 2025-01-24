package org.wso2.carbon.esb.module.ai.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;

import java.lang.reflect.Type;

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
}
