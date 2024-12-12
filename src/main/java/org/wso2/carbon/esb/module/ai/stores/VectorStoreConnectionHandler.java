package org.wso2.carbon.esb.module.ai.stores;

import org.apache.synapse.MessageContext;
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
            default:
                break;
        }
        return vectorStore;
    }
}
