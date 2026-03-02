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

package org.wso2.carbon.esb.module.ai.connections;

import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.esb.module.ai.Constants;
import org.wso2.carbon.esb.module.ai.mcp.MCPConnectionHandler;

import java.util.HashMap;

/**
 * MCP Connection Connector
 * Handles connection configuration for MCP servers
 */
public class MCPConnection extends AbstractConnector implements ManagedLifecycle {

    @Override
    public void connect(MessageContext messageContext) {
        String connectionType = getProperty(messageContext, Constants.CONNECTION_TYPE);
        String connectionName = getProperty(messageContext, Constants.CONNECTION_NAME);

        HashMap<String, String> connectionProperties = new HashMap<>();
        
        // MCP server URL
        connectionProperties.put(Constants.MCP_SERVER_URL, getProperty(messageContext, Constants.MCP_SERVER_URL));
        
        // Authentication configuration
        connectionProperties.put(Constants.MCP_AUTHENTICATION_TYPE, getProperty(messageContext, Constants.MCP_AUTHENTICATION_TYPE));
        connectionProperties.put(Constants.MCP_BEARER_TOKEN, getProperty(messageContext, Constants.MCP_BEARER_TOKEN));
        connectionProperties.put(Constants.MCP_CUSTOM_HEADERS, getProperty(messageContext, Constants.MCP_CUSTOM_HEADERS));
        
        // Optional timeout configurations
        connectionProperties.put(Constants.MCP_TIMEOUT, getProperty(messageContext, Constants.MCP_TIMEOUT));
        connectionProperties.put(Constants.MCP_MAX_CONNECTIONS, getProperty(messageContext, Constants.MCP_MAX_CONNECTIONS));
        connectionProperties.put(Constants.MCP_CONNECTION_TIMEOUT, getProperty(messageContext, Constants.MCP_CONNECTION_TIMEOUT));
        connectionProperties.put(Constants.MCP_SOCKET_TIMEOUT, getProperty(messageContext, Constants.MCP_SOCKET_TIMEOUT));

        // Build authentication headers
        HashMap<String, String> headers = buildAuthenticationHeaders(messageContext);
        
        // Create connection params with headers
        MCPConnectionParams mcpConnectionParams = new MCPConnectionParams(
                connectionName, connectionType, connectionProperties, headers
        );

        // Register the connection
        MCPConnectionHandler.addConnection(connectionName, mcpConnectionParams);

        // Clear sensitive properties for security
        clearSensitiveProperties(messageContext);
    }
    /**
     * Builds authentication headers based on authentication type and custom headers
     */
    private HashMap<String, String> buildAuthenticationHeaders(MessageContext messageContext) {
        HashMap<String, String> headers = new HashMap<>();
        
        try {
            // Add authentication header if Bearer Token is selected
            String authType = getProperty(messageContext, Constants.MCP_AUTHENTICATION_TYPE);
            if (Constants.AUTH_TYPE_BEARER.equals(authType)) {
                String bearerToken = getProperty(messageContext, Constants.MCP_BEARER_TOKEN);
                if (bearerToken != null && !bearerToken.trim().isEmpty()) {
                    headers.put("Authorization", "Bearer " + bearerToken);
                }
            }
            
            // Parse and add custom headers (format: key:value, one per line)
            String customHeaders = getProperty(messageContext, Constants.MCP_CUSTOM_HEADERS);
            if (customHeaders != null && !customHeaders.trim().isEmpty()) {
                String[] headerLines = customHeaders.split("\\r?\\n");
                for (String line : headerLines) {
                    line = line.trim();
                    if (!line.isEmpty() && line.contains(":")) {
                        int colonIndex = line.indexOf(":");
                        String key = line.substring(0, colonIndex).trim();
                        String value = line.substring(colonIndex + 1).trim();
                        if (!key.isEmpty() && !value.isEmpty()) {
                            headers.put(key, value);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("Error building MCP authentication headers: " + e.getMessage(), e);
        }
        
        return headers;
    }

    /**
     * Clear sensitive properties like API keys and tokens from message context
     */
    private void clearSensitiveProperties(MessageContext messageContext) {
        // Clear bearer token
        messageContext.setProperty(Constants.MCP_BEARER_TOKEN, null);
        
        // Clear other potentially sensitive data
        messageContext.setProperty(Constants.API_KEY, null);
    }

    @Override
    public void init(SynapseEnvironment synapseEnvironment) {
        // Initialization if needed
    }

    @Override
    public void destroy() {
        // Cleanup MCP connections on destroy
        MCPConnectionHandler.closeAllConnections();
    }

    public String getProperty(MessageContext messageContext, String key) {
        return messageContext.getProperty(key) != null ? messageContext.getProperty(key).toString() : null;
    }
}
