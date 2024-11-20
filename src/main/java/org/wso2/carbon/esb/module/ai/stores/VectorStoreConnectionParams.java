package org.wso2.carbon.esb.module.ai.stores;

import java.util.HashMap;

public class VectorStoreConnectionParams {

        String connectionName;
        String connectionType;
        HashMap<String, String> connectionProperties = new HashMap<String, String>();

        public VectorStoreConnectionParams(String connectionName, String connectionType, HashMap<String, String> connectionProperties) {
            this.connectionName = connectionName;
            this.connectionType = connectionType;
            this.connectionProperties = connectionProperties;
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
