/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.esb.module.ai.connections;

import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.esb.module.ai.Constants;
import org.wso2.carbon.esb.module.ai.stores.VectorStoreConnectionHandler;

import java.util.HashMap;
import java.util.Map;

public class VectorStoreConnection extends AbstractConnector implements ManagedLifecycle {

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        String connectionType = getProperty(messageContext, Constants.CONNECTION_TYPE);
        String connectionName = getProperty(messageContext, Constants.CONNECTION_NAME);

        HashMap<String, String> connectionProperties = new HashMap<>();
        connectionProperties.put(Constants.PERSISTENCE, getProperty(messageContext, Constants.PERSISTENCE));
        connectionProperties.put(Constants.URL, getProperty(messageContext, Constants.URL));
        connectionProperties.put(Constants.COLLECTION, getProperty(messageContext, Constants.COLLECTION));

        connectionProperties.put(Constants.API_KEY, getProperty(messageContext, Constants.API_KEY));
        connectionProperties.put(Constants.CLOUD, getProperty(messageContext, Constants.CLOUD));
        connectionProperties.put(Constants.REGION, getProperty(messageContext, Constants.REGION));
        connectionProperties.put(Constants.INDEX, getProperty(messageContext, Constants.INDEX));
        connectionProperties.put(Constants.NAMESPACE, getProperty(messageContext, Constants.NAMESPACE));
        connectionProperties.put(Constants.DIMENSION, getProperty(messageContext, Constants.DIMENSION));

        connectionProperties.put(Constants.HOST, getProperty(messageContext, Constants.HOST));
        connectionProperties.put(Constants.PORT, getProperty(messageContext, Constants.PORT));
        connectionProperties.put(Constants.DATABASE, getProperty(messageContext, Constants.DATABASE));
        connectionProperties.put(Constants.USER, getProperty(messageContext, Constants.USER));
        connectionProperties.put(Constants.PASSWORD, getProperty(messageContext, Constants.PASSWORD));
        connectionProperties.put(Constants.TABLE, getProperty(messageContext, Constants.TABLE));

        VectorStoreConnectionHandler.addConnection(
                connectionName, new ConnectionParams(connectionName, connectionType, connectionProperties));

        // Clear the apiKey property for security reasons
        messageContext.setProperty(Constants.API_KEY, null);
    }

    @Override
    public void init(SynapseEnvironment synapseEnvironment) {
    }

    @Override
    public void destroy() {
    }

    public String getProperty(MessageContext messageContext, String key) {
        return messageContext.getProperty(key) != null ? messageContext.getProperty(key).toString() : null;
    }
}
