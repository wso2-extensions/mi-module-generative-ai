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
import org.wso2.carbon.esb.module.ai.stores.VectorStoreConnectionHandler;

import java.util.HashMap;

public class VectorStoreConnection extends AbstractConnector implements ManagedLifecycle {

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        String connectionType = messageContext.getProperty("connectionType").toString();
        String connectionName = messageContext.getProperty("connectionName").toString();

        String persistence = messageContext.getProperty("persistence") != null ? messageContext.getProperty("persistence").toString() : null;
        String url = messageContext.getProperty("url") != null ? messageContext.getProperty("url").toString() : null;
        String collection = messageContext.getProperty("collection") != null ? messageContext.getProperty("collection").toString() : null;

        String apiKey = messageContext.getProperty("apiKey") != null ? messageContext.getProperty("apiKey").toString() : null;
        String cloud = messageContext.getProperty("cloud") != null ? messageContext.getProperty("cloud").toString() : null;
        String region = messageContext.getProperty("region") != null ? messageContext.getProperty("region").toString() : null;
        String index = messageContext.getProperty("index") != null ? messageContext.getProperty("index").toString() : null;
        String namespace = messageContext.getProperty("namespace") != null ? messageContext.getProperty("namespace").toString() : null;
        String dimension = messageContext.getProperty("dimension") != null ? messageContext.getProperty("dimension").toString() : null;

        String host = messageContext.getProperty("host") != null ? messageContext.getProperty("host").toString() : null;
        String port = messageContext.getProperty("port") != null ? messageContext.getProperty("port").toString() : null;
        String database = messageContext.getProperty("database") != null ? messageContext.getProperty("database").toString() : null;
        String user = messageContext.getProperty("user") != null ? messageContext.getProperty("user").toString() : "";
        String password = messageContext.getProperty("password") != null ? messageContext.getProperty("password").toString() : null;
        String table = messageContext.getProperty("table") != null ? messageContext.getProperty("table").toString() : null;

        HashMap<String, String> connectionProperties = new HashMap<>();
        connectionProperties.put("persistence", persistence);
        connectionProperties.put("url", url);
        connectionProperties.put("collection", collection);

        connectionProperties.put("apiKey", apiKey);
        connectionProperties.put("cloud", cloud);
        connectionProperties.put("region", region);
        connectionProperties.put("index", index);
        connectionProperties.put("namespace", namespace);
        connectionProperties.put("dimension", dimension);

        connectionProperties.put("host", host);
        connectionProperties.put("port", port);
        connectionProperties.put("database", database);
        connectionProperties.put("user", user);
        connectionProperties.put("password", password);
        connectionProperties.put("table", table);

        VectorStoreConnectionHandler.addConnection(
                connectionName, new ConnectionParams(connectionName, connectionType, connectionProperties));
    }

    @Override
    public void init(SynapseEnvironment synapseEnvironment) {
    }

    @Override
    public void destroy() {
    }
}
