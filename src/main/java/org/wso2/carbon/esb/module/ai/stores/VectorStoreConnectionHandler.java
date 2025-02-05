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

package org.wso2.carbon.esb.module.ai.stores;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.wso2.carbon.esb.module.ai.connections.ConnectionParams;
import org.wso2.micro.integrator.registry.MicroIntegratorRegistry;
import java.util.concurrent.ConcurrentHashMap;

public class VectorStoreConnectionHandler {

    private final static ConcurrentHashMap<String, ConnectionParams> connections = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<String, VectorStore> vectorStores = new ConcurrentHashMap<>();

    public static void addConnection(String connectionName, ConnectionParams connectionParams) {
        connections.computeIfAbsent(connectionName, k -> connectionParams);
    }

    public static VectorStore getVectorStore(String connectionName, MessageContext mc) {
        VectorStore vectorStore = null;
        ConnectionParams connectionParams = connections.get(connectionName);
        String key = connectionName + "|" + connectionParams.getConnectionType();
        switch (connectionParams.getConnectionType()) {
            case "MI_VECTOR_STORE":
                Boolean persistence = connectionParams.getConnectionProperty("persistence").equals("Enable");
                vectorStore = vectorStores.computeIfAbsent(key, k -> {
                    MicroIntegratorRegistry microIntegratorRegistry =
                            (MicroIntegratorRegistry) mc.getConfiguration().getRegistry();
                    return new MIVectorStore(connectionName, persistence, microIntegratorRegistry);
                });
                break;
            case "CHROMA_DB":
                vectorStore = vectorStores.computeIfAbsent(key, k -> new ChromaDB(
                        connectionParams.getConnectionProperty("url"),
                        connectionParams.getConnectionProperty("collection")));
                break;
            case "PINECONE":
                vectorStore = vectorStores.computeIfAbsent(key, k -> new Pinecone(
                        connectionParams.getConnectionProperty("apiKey"),
                        connectionParams.getConnectionProperty("namespace"),
                        connectionParams.getConnectionProperty("cloud"),
                        connectionParams.getConnectionProperty("region"),
                        connectionParams.getConnectionProperty("index"),
                        Integer.parseInt(connectionParams.getConnectionProperty("dimension"))));
                break;
            case "POSTGRE_SQL":
                vectorStore = vectorStores.computeIfAbsent(key, k -> {
                    try {
                        boolean status = PGVector.testConnection(
                                connectionParams.getConnectionProperty("host"),
                                Integer.parseInt(connectionParams.getConnectionProperty("port")),
                                connectionParams.getConnectionProperty("database"),
                                connectionParams.getConnectionProperty("user"),
                                connectionParams.getConnectionProperty("password"));
                        if (!status) {
                            throw new SynapseException("Error connecting to PGVector");
                        }

                        return new PGVector(
                                connectionParams.getConnectionProperty("host"),
                                connectionParams.getConnectionProperty("port"),
                                connectionParams.getConnectionProperty("database"),
                                connectionParams.getConnectionProperty("user"),
                                connectionParams.getConnectionProperty("password"),
                                connectionParams.getConnectionProperty("table"),
                                Integer.parseInt(connectionParams.getConnectionProperty("dimension")));
                    } catch (Exception e) {
                        throw new SynapseException("Error creating PGVector connection", e);
                    }
                });
                break;
            default:
                break;
        }
        return vectorStore;
    }
}
