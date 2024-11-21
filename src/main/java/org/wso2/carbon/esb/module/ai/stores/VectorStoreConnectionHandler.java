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
            case "MI_VECTOR_STORE":
                Boolean persistence = connectionParams.getConnectionProperty("persistence").equals("Enable");
                vectorStore = vectorStores.computeIfAbsent(key, k -> {
                    MicroIntegratorRegistry microIntegratorRegistry =
                            (MicroIntegratorRegistry) mc.getConfiguration().getRegistry();
                    return new MIVectorStore(connectionName, persistence, microIntegratorRegistry);
                });
            case "CHROMA_DB":
                vectorStore = vectorStores.computeIfAbsent(key, k -> new ChromaDB(connectionName,
                        connectionParams.getConnectionProperty("url"),
                        connectionParams.getConnectionProperty("collection")));
                break;
            default:
                break;
        }
        return vectorStore;
    }
}
