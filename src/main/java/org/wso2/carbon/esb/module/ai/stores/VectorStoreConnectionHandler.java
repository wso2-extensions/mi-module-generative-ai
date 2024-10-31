package org.wso2.carbon.esb.module.ai.stores;

import org.apache.synapse.MessageContext;
import org.wso2.micro.integrator.registry.MicroIntegratorRegistry;
import java.util.concurrent.ConcurrentHashMap;

public class VectorStoreConnectionHandler {

    private final static ConcurrentHashMap<String, VectorStoreConnectionParams> connections = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<String, VectorStore> vectorStores = new ConcurrentHashMap<>();

    public static void addConnection(String connectionName, VectorStoreConnectionParams connectionParams) {
        connections.computeIfAbsent(connectionName, k -> connectionParams);
    }

    public static VectorStoreConnectionParams getConnection(String connectionName) {
        return connections.get(connectionName);
    }

    public static VectorStore getVectorStore(String connectionName, MessageContext mc) {
        VectorStore vectorStore = null;
        VectorStoreConnectionParams connectionParams = connections.get(connectionName);
        String key = connectionName + "|" + connectionParams.getConnectionType();
        switch (connectionParams.getConnectionType()) {
            case "In-Memory":
                vectorStore = vectorStores.computeIfAbsent(key, k -> {
                    MicroIntegratorRegistry microIntegratorRegistry =
                            (MicroIntegratorRegistry) mc.getConfiguration().getRegistry();
                    return new InMemoryVectorStore(connectionName, microIntegratorRegistry);
                });
            case "Pine-cone":
                // To be implemented
                break;
            default:
                break;
        }
        return vectorStore;
    }
}
