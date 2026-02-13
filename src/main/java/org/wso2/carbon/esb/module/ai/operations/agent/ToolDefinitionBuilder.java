package org.wso2.carbon.esb.module.ai.operations.agent;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SequenceType;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.ValueFactory;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.template.InvokeMediator;
import org.apache.synapse.mediators.template.ResolvedInvokeParam;
import org.apache.synapse.mediators.template.TemplateMediator;
import org.apache.synapse.mediators.template.TemplateParam;
import org.apache.synapse.util.xpath.SynapseExpression;
import org.wso2.carbon.connector.core.util.ConnectorUtils;
import org.wso2.carbon.esb.module.ai.Constants;
import org.wso2.carbon.esb.module.ai.Errors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ToolDefinitionBuilder {

    protected Log log = LogFactory.getLog(this.getClass());

    public void generateToolSpecifications(MessageContext mc, ToolDefinitions toolDefinitions) {

        List<Tool> tools = getTools(mc);
        List<ToolSpecification> toolSpecifications = new ArrayList<>();
        if (!tools.isEmpty()) {
            for (Tool tool : tools) {
                // Check if this is an MCP tool
                if (Constants.MCP_TOOL_TYPE.equals(tool.getType())) {
                    // Handle MCP tool
                    generateMCPToolSpecification(tool, toolSpecifications, toolDefinitions);
                } else {
                    // Handle regular Synapse template tool
                    String toolTemplate = tool.getTemplate();
                    Mediator mediator = mc.getSequenceTemplate(toolTemplate);
                    if (mediator instanceof TemplateMediator templateMediator) {
                        String template = tool.getTemplate();
                        String description = templateMediator.getDescription();
                        List<TemplateParam> templateParams = new ArrayList<>(templateMediator.getParameters());
                        JsonObjectSchema parameterSchema = generateParameterSchema(templateParams);
                        ToolSpecification toolSpecification =
                                ToolSpecification.builder().name(template).description(description)
                                        .parameters(parameterSchema).build();
                        toolDefinitions.addToolResultExpression(template, tool.getResultExpression());
                        toolSpecifications.add(toolSpecification);

                        // Add invoker for tool
                        SequenceMediator toolInvoker = new SequenceMediator();
                        toolInvoker.setSequenceType(SequenceType.ANON);
                        InvokeMediator invoker = new InvokeMediator();
                        invoker.setTargetTemplate(toolTemplate);

                        toolInvoker.addChild(invoker);
                        toolDefinitions.addToolInvoker(template, toolInvoker);
                    }
                }
            }
        }
        toolDefinitions.addToolSpecifications(toolSpecifications);
    }

    private List<Tool> getTools(MessageContext mc) {

        Object toolsObject = ConnectorUtils.lookupTemplateParamater(mc, Constants.TOOLS);
        List<Tool> tools = new ArrayList<>();
        if (toolsObject instanceof ResolvedInvokeParam toolsParams) {
            List<ResolvedInvokeParam> toolList = toolsParams.getChildren();
            for (ResolvedInvokeParam toolParam : toolList) {
                Map<String, Object> toolAttributes = toolParam.getAttributes();
                String name = (String) toolAttributes.get(Constants.NAME);
                String type = (String) toolAttributes.get(Constants.TYPE);
                String description = (String) toolAttributes.get(Constants.DESCRIPTION);
                
                // Check if this is an MCP tool
                if (Constants.MCP_TOOL_TYPE.equals(type)) {
                    String mcpConnection = (String) toolAttributes.get(Constants.MCP_CONNECTION);
                    
                    if (StringUtils.isBlank(name) || StringUtils.isBlank(mcpConnection)) {
                        handleConnectorException(Errors.INVALID_TOOL_CONFIGURATION, mc);
                    }
                    
                    name = name.replaceAll("\\s", "_");
                    Tool tool = new Tool(name, type, mcpConnection, description);
                    tools.add(tool);
                } else {
                    // Regular Synapse template tool: requires template and resultExpression
                    String template = (String) toolAttributes.get(Constants.TEMPLATE);
                    String resultExpression = (String) toolAttributes.get(Constants.RESULT_EXPRESSION);
                    
                    if (StringUtils.isBlank(name) || StringUtils.isBlank(template) ||
                            StringUtils.isBlank(resultExpression)) {
                        handleConnectorException(Errors.INVALID_TOOL_CONFIGURATION, mc);
                    }
                    
                    name = name.replaceAll("\\s", "_");
                    SynapseExpression resultSynapseExpression =
                            new ValueFactory().createSynapseExpression(resultExpression);
                    Value resultExpressionValue = new Value(resultSynapseExpression);
                    Tool tool = new Tool(name, template, resultExpressionValue, description);
                    tools.add(tool);
                }
            }
        }
        return tools;
    }

    /**
     * Generate MCP tool specification by fetching tool details from MCP server
     */
    private void generateMCPToolSpecification(Tool tool, List<ToolSpecification> toolSpecifications,
                                              ToolDefinitions toolDefinitions) {
        try {
            String mcpConnection = tool.getMcpConnection();
            String toolName = tool.getName();
            
            // Get MCP client for the connection
            dev.langchain4j.mcp.client.McpClient mcpClient = 
                org.wso2.carbon.esb.module.ai.mcp.MCPConnectionHandler.getOrCreateClient(mcpConnection);
            
            // Fetch available tools from MCP server
            List<dev.langchain4j.agent.tool.ToolSpecification> mcpTools = mcpClient.listTools();
            
            // Find the specific tool by name
            dev.langchain4j.agent.tool.ToolSpecification mcpToolSpec = mcpTools.stream()
                    .filter(ts -> toolName.equals(ts.name()))
                    .findFirst()
                    .orElseThrow(() -> new SynapseException("MCP tool '" + toolName + 
                            "' not found on server '" + mcpConnection + "'. Available tools: " + 
                            mcpTools.stream().map(dev.langchain4j.agent.tool.ToolSpecification::name)
                                    .reduce((a, b) -> a + ", " + b).orElse("none")));
            
            ToolSpecification toolSpec = ToolSpecification.builder()
                    .name(tool.getName())
                    .description(tool.getDescription() != null && !tool.getDescription().isEmpty() ? 
                            tool.getDescription() : mcpToolSpec.description())
                    .parameters(mcpToolSpec.parameters())
                    .build();
            
            toolSpecifications.add(toolSpec);
            
            // Store MCP tool metadata for execution
            toolDefinitions.addMCPToolMapping(tool.getName(), mcpConnection);
            
            if (log.isDebugEnabled()) {
                log.debug("Tool schema: " + (mcpToolSpec.parameters() != null ? 
                        "provided" : "empty"));
            }
        } catch (Exception e) {
            log.error("Error generating MCP tool specification for '" + tool.getName() + "'", e);
            throw new SynapseException("Failed to generate MCP tool specification: " + e.getMessage(), e);
        }
    }

    private JsonObjectSchema generateParameterSchema(List<TemplateParam> templateParams) {

        JsonObjectSchema.Builder builder = JsonObjectSchema.builder();
        for (TemplateParam templateParam : templateParams) {
            String paramName = templateParam.getName();
            boolean isMandatory = templateParam.isMandatory();
            String parameterDescription = templateParam.getDescription();
            builder.addStringProperty(paramName, parameterDescription != null ? parameterDescription : paramName);
            if (isMandatory) {
                builder.required(paramName);
            }
        }
        return builder.build();
    }

    public void handleConnectorException(Errors code, MessageContext mc) {

        this.log.error(code.getMessage());

        mc.setProperty("ERROR_CODE", code.getCode());
        mc.setProperty("ERROR_MESSAGE", code.getMessage());
        throw new SynapseException(code.getMessage());
    }

    private static class Tool {

        private String name;
        private String type;  // null for regular tools, "mcp" for MCP tools
        private String template;  // For regular Synapse template tools
        private Value resultExpression;  // For regular Synapse template tools
        private String mcpConnection;  // For MCP tools
        private String description;

        // Constructor for regular Synapse template tools
        public Tool(String name, String template, Value resultExpression, String description) {
            this.name = name;
            this.type = null;  // Regular tool
            this.template = template;
            this.resultExpression = resultExpression;
            this.description = description;
        }

        // Constructor for MCP tools
        public Tool(String name, String type, String mcpConnection, String description) {
            this.name = name;
            this.type = type;
            this.mcpConnection = mcpConnection;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTemplate() {
            return template;
        }

        public void setTemplate(String template) {
            this.template = template;
        }

        public Value getResultExpression() {
            return resultExpression;
        }

        public void setResultExpression(Value resultExpression) {
            this.resultExpression = resultExpression;
        }

        public String getMcpConnection() {
            return mcpConnection;
        }

        public void setMcpConnection(String mcpConnection) {
            this.mcpConnection = mcpConnection;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
