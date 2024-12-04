package org.wso2.carbon.esb.module.ai.llm;

import java.util.HashMap;

public class LLMConnectionParams {

    String apiKey;
    String connectionName;
    String connectionType;
    HashMap<String, String> connectionProperties = new HashMap<String, String>();

    public LLMConnectionParams(String apiKey, String connectionName, String connectionType, HashMap<String, String> connectionProperties) {
        this.apiKey = apiKey;
        this.connectionName = connectionName;
        this.connectionType = connectionType;
        this.connectionProperties = connectionProperties;
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

    public HashMap<String, String> getConnectionProperties() {
        return connectionProperties;
    }

    public String getConnectionProperty(String key) {
        return connectionProperties.get(key);
    }
}
