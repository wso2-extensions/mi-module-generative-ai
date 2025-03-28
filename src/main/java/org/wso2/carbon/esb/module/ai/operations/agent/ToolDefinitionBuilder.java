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
                String description = (String) toolAttributes.get(Constants.DESCRIPTION);
                Tool
                        tool = new Tool(name, template, resultExpressionValue, description);
                tools.add(tool);
            }
        }
        return tools;
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
        private String template;
        private Value resultExpression;
        private String description;

        public Tool(String name, String template, Value resultExpression, String description) {

            this.name = name;
            this.template = template;
            this.resultExpression = resultExpression;
            this.description = description;
        }

        public String getName() {

            return name;
        }

        public void setName(String name) {

            this.name = name;
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

        public String getDescription() {

            return description;
        }

        public void setDescription(String description) {

            this.description = description;
        }
    }
}
