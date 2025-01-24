package org.wso2.carbon.esb.module.ai.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import dev.langchain4j.data.document.Metadata;

import java.lang.reflect.Type;

class MetadataSerializer implements JsonSerializer<Metadata> {
    @Override
    public JsonElement serialize(Metadata metadata, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(metadata.toMap()); // Serialize the map directly
    }
}
