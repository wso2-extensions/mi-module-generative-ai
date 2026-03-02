package org.wso2.carbon.esb.module.ai.operations.agent;

import dev.langchain4j.agent.tool.ToolSpecification;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.base.SequenceMediator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A container for tool definitions associated with an agent instance.
 * Provides access to tool results, invokers, and specifications.
 */
public class ToolDefinitions {

    private final Map<String, Value> toolResultExpression;
    private final Map<String, SequenceMediator> toolInvokers;
    private final List<ToolSpecification> toolSpecifications;
    private final Map<String, MCPToolMetadata> mcpToolMappings;  // Maps tool name to MCP metadata
    private long toolExecutionTimeout = 10000; // Default timeout: 10 seconds

    public ToolDefinitions() {

        this.toolResultExpression = new HashMap<>();
        this.toolInvokers = new HashMap<>();
        this.toolSpecifications = new ArrayList<>();
        this.mcpToolMappings = new HashMap<>();
    }

    public void addToolResultExpression(String toolName, Value value) {

        this.toolResultExpression.put(toolName, value);
    }

    public void addToolInvoker(String toolName, SequenceMediator sequenceMediator) {

        this.toolInvokers.put(toolName, sequenceMediator);
    }

    public void addToolSpecification(ToolSpecification toolSpecification) {

        this.toolSpecifications.add(toolSpecification);
    }

    public Map<String, Value> getToolResultExpression() {

        return toolResultExpression;
    }

    public Map<String, SequenceMediator> getToolInvokers() {

        return toolInvokers;
    }

    public List<ToolSpecification> getToolSpecifications() {

        return toolSpecifications;
    }

    public void addToolSpecifications(List<ToolSpecification> toolSpecifications) {

        this.toolSpecifications.addAll(toolSpecifications);
    }

    public SequenceMediator getToolInvoker(String name) {

        return toolInvokers.get(name);
    }

    public long getToolExecutionTimeout() {

        return toolExecutionTimeout;
    }

    public void setToolExecutionTimeout(long toolExecutionTimeout) {

        this.toolExecutionTimeout = toolExecutionTimeout;
    }

    /**
     * Add MCP tool mapping (tool name -> MCP connection)
     */
    public void addMCPToolMapping(String toolName, String mcpConnection) {
        this.mcpToolMappings.put(toolName, new MCPToolMetadata(mcpConnection));
    }

    /**
     * Get MCP tool metadata for a given tool name
     */
    public MCPToolMetadata getMCPToolMetadata(String toolName) {
        return mcpToolMappings.get(toolName);
    }

    /**
     * Check if a tool is an MCP tool
     */
    public boolean isMCPTool(String toolName) {
        return mcpToolMappings.containsKey(toolName);
    }

    /**
     * Get all MCP tool mappings
     */
    public Map<String, MCPToolMetadata> getMcpToolMappings() {
        return mcpToolMappings;
    }

    /**
     * Metadata for MCP tools
     */
    public static class MCPToolMetadata {
        private final String mcpConnection;

        public MCPToolMetadata(String mcpConnection) {
            this.mcpConnection = mcpConnection;
        }

        public String getMcpConnection() {
            return mcpConnection;
        }
    }
}
