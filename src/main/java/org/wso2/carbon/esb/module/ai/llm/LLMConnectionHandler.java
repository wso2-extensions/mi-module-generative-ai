package org.wso2.carbon.esb.module.ai.llm;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class LLMConnectionHandler {

    private final static ConcurrentHashMap<String, LLMConnectionParams> connectionMap = new ConcurrentHashMap<>();

    public static void addConnection(String connectionName, LLMConnectionParams connectionParams) {
        connectionMap.computeIfAbsent(connectionName, k -> connectionParams);
    }

    public static LLMConnectionParams getConnection(String connectionName) {
        return connectionMap.get(connectionName);
    }

    public static ChatLanguageModel getChatModel(
            String connectionName, String modelName, Double temperature, Integer maxTokens,
            Double topP, Double frequencyPenalty, Integer seed) {

        ChatLanguageModel chatModel = null;
        LLMConnectionParams connectionParams = connectionMap.get(connectionName);
        switch (Objects.requireNonNull(connectionParams).getConnectionType()) {
            case "OPEN_AI":
                // Null values of LLM params will be handled by LangChain4j
                 chatModel =  OpenAiChatModel.builder()
                         .modelName(modelName)
                         .temperature(temperature)
                         .maxTokens(maxTokens)
                         .topP(topP)
                         .frequencyPenalty(frequencyPenalty)
                         .seed(seed)
                         .apiKey(connectionParams.getApiKey())
                         .build();
                 break;
            case "AZURE_OPEN_AI":
                // Null values of LLM params will be handled by LangChain4j
                chatModel =  AzureOpenAiChatModel.builder()
                        .temperature(temperature)
                        .maxTokens(maxTokens)
                        .topP(topP)
                        .frequencyPenalty(frequencyPenalty)
                        .seed(Long.valueOf(seed))
                        .apiKey(connectionParams.getApiKey())
                        .deploymentName(connectionParams.getConnectionProperty("deploymentName"))
                        .endpoint(connectionParams.getConnectionProperty("endpoint"))
                        .build();
                break;
            case "ANTHROPIC":
                chatModel = AnthropicChatModel.builder()
                        .modelName(modelName)
                        .temperature(temperature)
                        .maxTokens(maxTokens)
                        .topP(topP)
                        .apiKey(connectionParams.getApiKey())
                        .build();
                break;
            case "MISTRAL_AI":
                chatModel = MistralAiChatModel.builder()
                        .modelName(modelName)
                        .temperature(temperature)
                        .maxTokens(maxTokens)
                        .topP(topP)
                        .apiKey(connectionParams.getApiKey())
                        .build();
                break;
            default:
                break;
        }
        return chatModel;
    }

    public static EmbeddingModel getEmbeddingModel(String connectionName, String modelName) {
        EmbeddingModel embeddingModel = null;
        LLMConnectionParams connectionParams = connectionMap.get(connectionName);
        switch (Objects.requireNonNull(connectionParams).getConnectionType()) {
            case "OPEN_AI":
                // Null values of LLM params will be handled by LangChain4j
                embeddingModel = OpenAiEmbeddingModel.builder()
                        .apiKey(connectionParams.getApiKey())
                        .modelName(modelName)
                        .build();
            case "ANTHROPIC":
                // To be implemented
                break;
            default:
                break;
        }
        return embeddingModel;
    }
}
