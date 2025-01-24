package org.wso2.carbon.esb.module.ai.utils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import dev.langchain4j.data.document.Metadata;

import java.lang.reflect.Type;
import java.util.Map;

class MetadataDeserializer implements JsonDeserializer<Metadata> {
    @Override
    public Metadata deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        // Deserialize the JSON back into the internal map
        Map<String, Object> metadataMap = context.deserialize(json, Map.class);
        return new Metadata(metadataMap);
    }
}
