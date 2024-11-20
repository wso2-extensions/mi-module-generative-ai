package org.wso2.carbon.esb.module.ai.connections;

import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.esb.module.ai.stores.VectorStoreConnectionHandler;
import org.wso2.carbon.esb.module.ai.stores.VectorStoreConnectionParams;

import java.util.HashMap;

public class vectorStoreConnection extends AbstractConnector implements ManagedLifecycle {

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        String connectionType = messageContext.getProperty("connectionType").toString();
        String connectionName = messageContext.getProperty("connectionName").toString();
        String persistence = messageContext.getProperty("persistence").toString();

        HashMap<String, String> connectionProperties = new HashMap<>();
        connectionProperties.put("persistence", persistence);

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
