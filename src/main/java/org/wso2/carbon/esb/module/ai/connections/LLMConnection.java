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
import org.wso2.carbon.esb.module.ai.Constants;
import org.wso2.carbon.esb.module.ai.llm.LLMConnectionHandler;

import java.util.HashMap;

public class LLMConnection extends AbstractConnector implements ManagedLifecycle {

    @Override
    public void connect(MessageContext messageContext) {
        String connectionType = getProperty(messageContext, Constants.CONNECTION_TYPE);
        String connectionName = getProperty(messageContext, Constants.CONNECTION_NAME);

        HashMap<String, String> connectionProperties = new HashMap<>();
        connectionProperties.put(Constants.API_KEY, getProperty(messageContext, Constants.API_KEY));
        connectionProperties.put(Constants.DEPLOYMENT_NAME, getProperty(messageContext, Constants.DEPLOYMENT_NAME));
        connectionProperties.put(Constants.ENDPOINT, getProperty(messageContext, Constants.ENDPOINT));
        connectionProperties.put(Constants.BASE_URL, getProperty(messageContext, Constants.BASE_URL));

        LLMConnectionHandler.addConnection(
                connectionName, new ConnectionParams(connectionName, connectionType, connectionProperties)
        );

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
