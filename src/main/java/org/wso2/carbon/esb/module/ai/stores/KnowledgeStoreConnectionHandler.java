package org.wso2.carbon.esb.module.ai.stores;

import org.apache.synapse.MessageContext;
import org.wso2.micro.integrator.registry.MicroIntegratorRegistry;
import java.util.concurrent.ConcurrentHashMap;

public class KnowledgeStoreConnectionHandler {

    private final static ConcurrentHashMap<String, KnowledgeStoreConnectionParams> connections = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<String, KnowledgeStore> knowledgeStores = new ConcurrentHashMap<>();

    public static void addConnection(String connectionName, KnowledgeStoreConnectionParams connectionParams) {
        connections.computeIfAbsent(connectionName, k -> connectionParams);
    }

    public static KnowledgeStoreConnectionParams getConnection(String connectionName) {
        return connections.get(connectionName);
    }

    public static KnowledgeStore getKnowledgeStore(String connectionName, MessageContext mc) {
        KnowledgeStore knowledgeStore = null;
        KnowledgeStoreConnectionParams connectionParams = connections.get(connectionName);
        String key = connectionName + "|" + connectionParams.getConnectionType();
        switch (connectionParams.getConnectionType()) {
            case "In-Memory":
                knowledgeStore = knowledgeStores.computeIfAbsent(key, k -> {
                    MicroIntegratorRegistry microIntegratorRegistry =
                            (MicroIntegratorRegistry) mc.getConfiguration().getRegistry();
                    return new InMemoryKnowledgeStore(key, microIntegratorRegistry);
                });
            case "Pine-cone":
                // To be implemented
                break;
            default:
                break;
        }
        return knowledgeStore;
    }
}
