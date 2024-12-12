package org.wso2.carbon.esb.module.ai.connections;

import org.apache.synapse.SynapseException;

import java.util.HashMap;

public class ConnectionParams {

    String connectionName;
    String connectionType;
    HashMap<String, String> connectionProperties;

    public ConnectionParams(String connectionName, String connectionType, HashMap<String, String> connectionProperties) {
        this.connectionName = connectionName;
        this.connectionType = connectionType;
        this.connectionProperties = connectionProperties;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public String getConnectionProperty(String key) {
        String property = connectionProperties.get(key);
        if (property == null) {
            throw new SynapseException("Property " + key + " is not set for connection " + connectionName);
        }
        return property;
    }
}
