package org.wso2.carbon.esb.module.ai.stores;

public class KnowledgeStoreConnectionParams {

        String connectionName;
        String connectionType;

        public KnowledgeStoreConnectionParams(String connectionName, String connectionType) {
            this.connectionName = connectionName;
            this.connectionType = connectionType;
        }

        public String getConnectionName() {
            return connectionName;
        }

        public String getConnectionType() {
            return connectionType;
        }
}
