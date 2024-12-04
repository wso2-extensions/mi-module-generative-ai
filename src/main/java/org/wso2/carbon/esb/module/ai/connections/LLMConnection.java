package org.wso2.carbon.esb.module.ai.connections;

import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.esb.module.ai.llm.LLMConnectionHandler;
import org.wso2.carbon.esb.module.ai.llm.LLMConnectionParams;

import java.util.HashMap;

public class LLMConnection extends AbstractConnector implements ManagedLifecycle {

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        String apiKey = messageContext.getProperty("apiKey").toString();
        String connectionType = messageContext.getProperty("connectionType").toString();
        String connectionName = messageContext.getProperty("connectionName").toString();

        String deploymentName = messageContext.getProperty("deploymentName") != null ? messageContext.getProperty("persistence").toString() : null;
        String endpoint = messageContext.getProperty("endpoint") != null ? messageContext.getProperty("url").toString() : null;

        HashMap<String, String> connectionProperties = new HashMap<>();
        connectionProperties.put("deploymentName", deploymentName);
        connectionProperties.put("endpoint", endpoint);

        LLMConnectionHandler.addConnection(connectionName, new LLMConnectionParams(apiKey, connectionName, connectionType, connectionProperties));
        // Clear the apiKey property for security reasons
        messageContext.setProperty("apiKey", "");
    }

    @Override
    public void init(SynapseEnvironment synapseEnvironment) {
    }

    @Override
    public void destroy() {
    }
}
