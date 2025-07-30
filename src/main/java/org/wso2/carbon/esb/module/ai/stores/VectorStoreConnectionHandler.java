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

import org.apache.commons.lang3.StringUtils;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.Constants;
import org.wso2.carbon.esb.module.ai.Errors;
import org.wso2.carbon.esb.module.ai.connections.ConnectionParams;
import org.wso2.carbon.esb.module.ai.exceptions.VectorStoreException;
import org.wso2.micro.integrator.registry.MicroIntegratorRegistry;
import java.util.concurrent.ConcurrentHashMap;

public class VectorStoreConnectionHandler {

    private final static ConcurrentHashMap<String, ConnectionParams> connections = new ConcurrentHashMap<>();

    public static void addConnection(String connectionName, ConnectionParams connectionParams) {
        connections.computeIfAbsent(connectionName, k -> connectionParams);
    }

    public static VectorStore getVectorStore(String connectionName, MessageContext mc) throws VectorStoreException {

        VectorStore vectorStore = null;
        ConnectionParams connectionParams = connections.get(connectionName);
        if (connectionParams == null) {
            return null;
        }

        switch (connectionParams.getConnectionType()) {
            case Constants.MI_VECTOR_STORE:
                MicroIntegratorRegistry microIntegratorRegistry = (MicroIntegratorRegistry) mc.getConfiguration().getRegistry();
                vectorStore = new MIVectorStore(connectionName, microIntegratorRegistry);
                break;

            case Constants.CHROMA_DB:
                vectorStore = new ChromaDB(
                        connectionParams.getConnectionProperty(Constants.URL),
                        connectionParams.getConnectionProperty(Constants.COLLECTION)
                );
                break;

            case Constants.PINECONE:
                try {
                    String namespace = connectionParams.getConnectionProperty(Constants.NAMESPACE, true);
                    if (namespace == null || Pinecone.DEFAULT_NAMESPACE.equals(namespace)) {
                        namespace = StringUtils.EMPTY;
                    }
                    vectorStore = new Pinecone(
                            connectionParams.getConnectionProperty(Constants.API_KEY),
                            namespace,
                            connectionParams.getConnectionProperty(Constants.CLOUD),
                            connectionParams.getConnectionProperty(Constants.REGION),
                            connectionParams.getConnectionProperty(Constants.INDEX),
                            Integer.parseInt(connectionParams.getConnectionProperty(Constants.DIMENSION)));
                } catch (Exception e) {
                    throw new VectorStoreException(Errors.PINECONE_CONNECTION_ERROR, e);
                }
                break;

            case Constants.POSTGRES_VECTOR:
                try {
                    boolean status = PGVector.testConnection(
                            connectionParams.getConnectionProperty(Constants.HOST),
                            Integer.parseInt(connectionParams.getConnectionProperty(Constants.PORT)),
                            connectionParams.getConnectionProperty(Constants.DATABASE),
                            connectionParams.getConnectionProperty(Constants.USER),
                            connectionParams.getConnectionProperty(Constants.PASSWORD));
                    if (!status) {
                        throw new VectorStoreException(Errors.POSTGRE_SQL_CONNECTION_ERROR);
                    }

                    vectorStore = new PGVector(
                            connectionParams.getConnectionProperty(Constants.HOST),
                            connectionParams.getConnectionProperty(Constants.PORT),
                            connectionParams.getConnectionProperty(Constants.DATABASE),
                            connectionParams.getConnectionProperty(Constants.USER),
                            connectionParams.getConnectionProperty(Constants.PASSWORD),
                            connectionParams.getConnectionProperty(Constants.TABLE),
                            Integer.parseInt(connectionParams.getConnectionProperty(Constants.DIMENSION)));
                } catch (Exception e) {
                    throw new VectorStoreException(Errors.POSTGRE_SQL_CONNECTION_ERROR, e);
                }
                break;

            default:
                break;
        }
        return vectorStore;
    }
}
