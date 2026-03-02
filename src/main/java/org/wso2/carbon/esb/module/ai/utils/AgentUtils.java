package org.wso2.carbon.esb.module.ai.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgentUtils {

    private static final Gson GSON = new Gson();
    /**
     * Utility {@link TypeToken} describing {@code Map<String, Object>}.
     */
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();
    private static final Pattern TRAILING_COMMA_PATTERN = Pattern.compile(",(\\s*[}\\]])");

    public static Map<String, Object> argumentsAsMap(String arguments) {

        return GSON.fromJson(removeTrailingComma(arguments), MAP_TYPE);
    }

    /**
     * Removes trailing commas before closing braces or brackets in JSON strings.
     *
     * @param json the JSON string
     * @return the corrected JSON string
     */
    static String removeTrailingComma(String json) {

        if (json == null || json.isEmpty()) {
            return json;
        }
        Matcher matcher = TRAILING_COMMA_PATTERN.matcher(json);
        return matcher.replaceAll("$1");
    }

    public static void addSystemMessageIfMissing(List<ChatMessage> messages, Function<Object, String> systemMessageProvider,
                                                    String memoryId, String defaultSystemPrompt) {

        if (!(messages.get(0) instanceof SystemMessage)) {
            String systemPromptText = systemMessageProvider != null ? systemMessageProvider.apply(memoryId) : defaultSystemPrompt;
            SystemMessage systemMessage = new SystemMessage(systemPromptText != null ? systemPromptText : defaultSystemPrompt);
            messages.add(0, systemMessage);
        }
        Iterator<ChatMessage> iterator = messages.iterator();

        // Skip the first message as it is a system message
        if (iterator.hasNext()) {
            iterator.next();
        }

        while (iterator.hasNext()) {
            ChatMessage message = iterator.next();
            if (message instanceof SystemMessage) {
                iterator.remove();
            }
        }
    }
}
