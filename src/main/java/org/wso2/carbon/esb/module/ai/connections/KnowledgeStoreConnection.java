package org.wso2.carbon.esb.module.ai.connections;

import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;

public class KnowledgeStoreConnection extends AbstractConnector implements ManagedLifecycle {

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        String connectionType = messageContext.getProperty("connectionType").toString();
        String connectionName = messageContext.getProperty("connectionName").toString();
        KnowledgeStoreConnectionHandler.addConnection(connectionName, new KnowledgeStoreConnectionParams(connectionName, connectionType));
    }

    @Override
    public void init(SynapseEnvironment synapseEnvironment) {
    }

    @Override
    public void destroy() {
    }
}
