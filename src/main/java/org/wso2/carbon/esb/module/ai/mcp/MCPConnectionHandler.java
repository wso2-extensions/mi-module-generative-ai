/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.esb.module.ai.mcp;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.esb.module.ai.Constants;
import org.wso2.carbon.esb.module.ai.connections.MCPConnectionParams;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler for managing MCP client connections
 * Provides connection pooling and lifecycle management for MCP clients
 */
public class MCPConnectionHandler {

    private static final Log log = LogFactory.getLog(MCPConnectionHandler.class);

    // Connection name -> MCPConnectionParams
    private static final ConcurrentHashMap<String, MCPConnectionParams> connectionParamsMap = new ConcurrentHashMap<>();

    // Connection name -> McpClient (cached clients)
    private static final ConcurrentHashMap<String, McpClient> clientCache = new ConcurrentHashMap<>();

    // Track last access time for cleanup
    private static final ConcurrentHashMap<String, Long> lastAccessTime = new ConcurrentHashMap<>();

    // Cache expiry time (5 minutes)
    private static final long CACHE_EXPIRY_MS = 300000;

    /**
     * Register an MCP connection configuration
     */
    public static void addConnection(String connectionName, MCPConnectionParams connectionParams) {
        connectionParamsMap.computeIfAbsent(connectionName, k -> connectionParams);
        if (log.isDebugEnabled()) {
            log.debug("Registered MCP connection: " + connectionName);
        }
    }

    /**
     * Get or create an MCP client for the given connection name
     */
    public static McpClient getOrCreateClient(String connectionName) throws Exception {
        MCPConnectionParams params = connectionParamsMap.get(connectionName);
        if (params == null) {
            throw new IllegalArgumentException("MCP connection not found: " + connectionName);
        }

        // Check if client exists in cache
        McpClient client = clientCache.get(connectionName);
        if (client != null) {
            lastAccessTime.put(connectionName, System.currentTimeMillis());
            return client;
        }

        // Create new client
        synchronized (MCPConnectionHandler.class) {
            // Double-check after acquiring lock
            client = clientCache.get(connectionName);
            if (client != null) {
                lastAccessTime.put(connectionName, System.currentTimeMillis());
                return client;
            }

            client = createClient(params);
            clientCache.put(connectionName, client);
            lastAccessTime.put(connectionName, System.currentTimeMillis());

            if (log.isDebugEnabled()) {
                log.debug("Created new MCP client for connection: " + connectionName);
            }

            return client;
        }
    }

    /**
     * Create a new MCP client from connection parameters
     */
    private static McpClient createClient(MCPConnectionParams params) throws Exception {
        String serverUrl = params.getConnectionProperty(Constants.MCP_SERVER_URL);
        if (serverUrl == null || serverUrl.isEmpty()) {
            throw new IllegalArgumentException("MCP server URL is required");
        }

        // Build transport with headers
        StreamableHttpMcpTransport.Builder transportBuilder = StreamableHttpMcpTransport.builder()
                .url(serverUrl);

        // Add custom headers (for authentication, etc.)
        if (params.hasHeaders()) {
            Map<String, String> headers = params.getHeaders();
            transportBuilder.customHeaders(headers);

            if (log.isDebugEnabled()) {
                log.debug("Added " + headers.size() + " custom headers to MCP transport");
            }
        }

        StreamableHttpMcpTransport transport = transportBuilder.build();

        // Build MCP client
        McpClient client = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        if (log.isDebugEnabled()) {
            log.debug("Created MCP client for server: " + serverUrl);
        }

        return client;
    }

    /**
     * Get connection parameters for a given connection name
     */
    public static MCPConnectionParams getConnectionParams(String connectionName) {
        return connectionParamsMap.get(connectionName);
    }

    /**
     * Close a specific MCP client connection
     */
    public static void closeConnection(String connectionName) {
        McpClient client = clientCache.remove(connectionName);
        lastAccessTime.remove(connectionName);

        if (client != null) {
            try {
                client.close();
                if (log.isDebugEnabled()) {
                    log.debug("Closed MCP client for connection: " + connectionName);
                }
            } catch (Exception e) {
                log.warn("Error closing MCP client for connection: " + connectionName, e);
            }
        }
    }

    /**
     * Close all MCP client connections
     */
    public static void closeAllConnections() {
        for (String connectionName : clientCache.keySet()) {
            closeConnection(connectionName);
        }
        if (log.isDebugEnabled()) {
            log.debug("Closed all MCP client connections");
        }
    }

    /**
     * Clean up stale connections that haven't been accessed recently
     */
    public static void cleanupStaleConnections() {
        long now = System.currentTimeMillis();
        clientCache.keySet().removeIf(connectionName -> {
            Long lastAccess = lastAccessTime.get(connectionName);
            if (lastAccess != null && (now - lastAccess) > CACHE_EXPIRY_MS) {
                if (log.isDebugEnabled()) {
                    log.debug("Removing stale MCP connection: " + connectionName);
                }
                closeConnection(connectionName);
                return true;
            }
            return false;
        });
    }

    /**
     * Get cache statistics (for monitoring/debugging)
     */
    public static Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConnections", connectionParamsMap.size());
        stats.put("activeCachedClients", clientCache.size());
        stats.put("cacheExpiryMs", CACHE_EXPIRY_MS);
        return stats;
    }
}
