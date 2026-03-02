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

package org.wso2.carbon.esb.module.ai.config;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.template.ResolvedInvokeParam;
import org.wso2.carbon.esb.module.ai.Constants;
import org.wso2.carbon.esb.module.ai.connections.MCPConnection;

import java.util.Iterator;
import java.util.List;

/**
 * Mediator to invoke MCP connection local entries
 * Reads the _MCP_CONNECTIONS property set by Configure and invokes each connection's local entry
 * by loading the local entry XML, extracting parameters, and directly calling MCPConnection
 */
public class InvokeMCPConnections extends AbstractMediator {

    private static final Log log = LogFactory.getLog(InvokeMCPConnections.class);

    @Override
    public boolean mediate(MessageContext messageContext) {
        
        // Get the MCP connections from the property set by Configure
        Object mcpConnectionsParam = messageContext.getProperty(Constants.MCP_CONNECTIONS);
        
        if (mcpConnectionsParam == null) {
            log.info("[MCP_INIT] No MCP connections property found");
            return true;
        }

        if (!(mcpConnectionsParam instanceof ResolvedInvokeParam)) {
            log.warn("[MCP_INIT] Invalid MCP connections configuration");
            return true;
        }

        ResolvedInvokeParam mcpConnections = (ResolvedInvokeParam) mcpConnectionsParam;
        List<ResolvedInvokeParam> connectionList = mcpConnections.getChildren();
        
        if (connectionList == null || connectionList.isEmpty()) {
            log.info("[MCP_INIT] MCP connections list is empty");
            return true;
        }

        log.info("[MCP_INIT] Processing " + connectionList.size() + " MCP connection(s)");

        // Invoke each MCP connection
        for (ResolvedInvokeParam connectionParam : connectionList) {
            Object connectionKey = connectionParam.getInlineValue();
            
            if (connectionKey instanceof String) {
                String configKey = (String) connectionKey;

                
                try {
                    // Load the local entry XML
                    Object localEntry = messageContext.getEntry(configKey);
                    
                    if (localEntry == null) {
                        log.error("[MCP_INIT] Local entry not found: " + configKey);
                        continue;
                    }
                    
                    if (!(localEntry instanceof OMElement)) {
                        log.error("[MCP_INIT] Local entry is not OMElement: " + configKey);
                        continue;
                    }
                    
                    OMElement localEntryElement = (OMElement) localEntry;
                    
                    // Extract connectionType and connectionName directly from ai.init children
                    String connectionType = getChildElementText(localEntryElement, "connectionType");
                    String connectionName = getChildElementText(localEntryElement, "name");
                    
                    if (connectionType == null || connectionName == null) {
                        log.error("[MCP_INIT] Missing connectionType or name in local entry: " + configKey);
                        continue;
                    }
                    
                    // Set properties in message context for MCPConnection to read
                    messageContext.setProperty(Constants.CONNECTION_TYPE, connectionType);
                    messageContext.setProperty(Constants.CONNECTION_NAME, connectionName);
                    
                    // Extract and set all parameter children directly from ai.init
                    Iterator<OMElement> paramIterator = localEntryElement.getChildElements();
                    while (paramIterator.hasNext()) {
                        OMElement param = paramIterator.next();
                        String paramName = param.getLocalName();
                        String paramValue = param.getText();
                        
                        // Skip connectionType and name as they're already set
                        if (!"connectionType".equals(paramName) && !"name".equals(paramName)) {
                            if (paramValue != null && !paramValue.trim().isEmpty()) {
                                messageContext.setProperty(paramName, paramValue);
                            }
                        }
                    }
                    
                    // Create and invoke MCPConnection
                    MCPConnection mcpConnection = new MCPConnection();
                    mcpConnection.connect(messageContext);
                    
                    log.info("[MCP_INIT] Successfully registered MCP connection: " + connectionName);
                    
                } catch (Exception e) {
                    log.error("[MCP_INIT] Error processing MCP connection: " + configKey, e);
                }
            }
        }

        return true;
    }
    
    /**
     * Helper method to get text content of a child element (namespace-agnostic)
     */
    private String getChildElementText(OMElement parent, String childName) {
        java.util.Iterator<OMElement> children = parent.getChildElements();
        while (children.hasNext()) {
            OMElement child = children.next();
            if (childName.equals(child.getLocalName())) {
                return child.getText();
            }
        }
        return null;
    }
}
