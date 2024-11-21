package org.wso2.carbon.esb.module.ai.connections;

import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.esb.module.ai.stores.VectorStoreConnectionHandler;
import org.wso2.carbon.esb.module.ai.stores.VectorStoreConnectionParams;

import java.util.HashMap;

public class VectorStoreConnection extends AbstractConnector implements ManagedLifecycle {

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        String connectionType = messageContext.getProperty("connectionType").toString();
        String connectionName = messageContext.getProperty("connectionName").toString();

        String persistence = messageContext.getProperty("persistence") != null ? messageContext.getProperty("persistence").toString() : null;
        String url = messageContext.getProperty("url") != null ? messageContext.getProperty("url").toString() : null;
        String collection = messageContext.getProperty("collection") != null ? messageContext.getProperty("collection").toString() : null;

        HashMap<String, String> connectionProperties = new HashMap<>();
        connectionProperties.put("persistence", persistence);
        connectionProperties.put("url", url);
        connectionProperties.put("collection", collection);

        VectorStoreConnectionHandler.addConnection(
                connectionName, new VectorStoreConnectionParams(connectionName, connectionType, connectionProperties));
    }

    @Override
    public void init(SynapseEnvironment synapseEnvironment) {
    }

    @Override
    public void destroy() {
    }
}
