package org.wso2.carbon.esb.module.ai.llm;

public class LLMConnectionParams {

    String apiKey;
    String connectionName;
    String connectionType;

    public LLMConnectionParams(String apiKey, String connectionName, String connectionType) {
        this.apiKey = apiKey;
        this.connectionName = connectionName;
        this.connectionType = connectionType;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public String getConnectionType() {
        return connectionType;
    }
}
