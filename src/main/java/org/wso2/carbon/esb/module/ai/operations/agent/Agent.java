/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.esb.module.ai.operations.agent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServiceContext;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.output.ServiceOutputParser;
import dev.langchain4j.service.tool.ToolExecution;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.OperationContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ContinuationState;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.continuation.ContinuationStackManager;
import org.apache.synapse.continuation.SeqContinuationState;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.FlowContinuableMediator;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.eip.EIPConstants;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.mediators.eip.SharedDataHolder;
import org.apache.synapse.mediators.template.TemplateContext;
import org.apache.synapse.util.MessageHelper;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;
import org.wso2.carbon.esb.module.ai.Constants;
import org.wso2.carbon.esb.module.ai.Errors;
import org.wso2.carbon.esb.module.ai.llm.LLMConnectionHandler;
import org.wso2.carbon.esb.module.ai.operations.agent.context.SharedAgentDataHolder;
import org.wso2.carbon.esb.module.ai.operations.agent.context.ToolExecutionDataHolder;
import org.wso2.carbon.esb.module.ai.utils.AgentUtils;
import org.wso2.carbon.esb.module.ai.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Timer;

/**
 * Language model chat operation
 * Inputs:
 * - modelName: Name of the language model
 * - userId: Unique user identifier
 * - temperature: Sampling temperature
 * - maxTokens: Maximum tokens to generate
 * - topP: Top P value
 * - frequencyPenalty: Frequency penalty
 * - seed: Random seed
 * - system: System message
 * - prompt: User message
 * - knowledge: JSON array of TextSegment objects
 * - history: JSON array of ChatMessage objects
 * - maxHistory: Maximum history size
 * - connectionName: Name of the connection to the LLM
 * Outputs:
 * - Response based on the output type
 */
public class Agent extends AbstractAIMediator implements FlowContinuableMediator {

    protected Log log = LogFactory.getLog(this.getClass());
    private static final int MAX_TOOL_EXECUTIONS_PER_REQUEST = 100;
    private static final String DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant.";
    private final ServiceOutputParser serviceOutputParser = new ServiceOutputParser();

    /**
     * invokeMediatorID -> [correlationId -> ToolExecutionAggregate]
     */
    private final Map<String, Map<String, ToolExecutionAggregate>> activeAggregates =
            Collections.synchronizedMap(new HashMap<>());

    /**
     * invokeMediatorID -> lock (lock per agent instance)
     */
    private final Map<String, Object> lockMap = Collections.synchronizedMap(new HashMap<>());

    /**
     * invokeMediatorID -> ToolDefinitions
     */
    private final Map<String, ToolDefinitions> toolDefinitionsMap = new HashMap<>();

    public Object getLock(String agentId) {

        return lockMap.computeIfAbsent(agentId, key -> new Object());
    }

    @Override
    public boolean mediate(MessageContext mc) {

        String agentID = getMediatorParameter(mc, SynapseConstants.INVOKE_MEDIATOR_ID, String.class, false);
        String connectionName = getProperty(mc, Constants.CONNECTION_NAME, String.class, false);

        if (!toolDefinitionsMap.containsKey(agentID)) {
            synchronized (getLock(agentID)) {
                if (!toolDefinitionsMap.containsKey(agentID)) {
                    new ToolDefinitionBuilder().generateToolSpecifications(mc, toolDefinitionsMap.compute(agentID,
                            (key, value) -> new ToolDefinitions()));
                }
            }
        }

        String modelName = getMediatorParameter(mc, Constants.MODEL_NAME, String.class, false);
        String responseVariable = getMediatorParameter(
                mc, Constants.RESPONSE_VARIABLE, String.class, false);
        Boolean overWriteBody = getMediatorParameter(mc, Constants.OVERWRITE_BODY, Boolean.class, true);

        // Advanced configurations
        String role = getMediatorParameter(mc, Constants.ROLE, String.class, true);
        String instructions = getMediatorParameter(mc, Constants.INSTRUCTIONS, String.class, true);
        String system = buildSystemPrompt(role, instructions, mc);

        Double temperature = getMediatorParameter(mc, Constants.TEMPERATURE, Double.class, true);
        Integer maxTokens = getMediatorParameter(mc, Constants.MAX_TOKENS, Integer.class, true);
        Double topP = getMediatorParameter(mc, Constants.TOP_P, Double.class, true);
        Double frequencyPenalty = getMediatorParameter(mc, Constants.FREQUENCY_PENALTY, Double.class, true);
        Integer seed = getMediatorParameter(mc, Constants.SEED, Integer.class, true);
        Integer maxChatHistory = getMediatorParameter(mc, Constants.MAX_HISTORY, Integer.class, true);
        if (maxChatHistory == null) {
            maxChatHistory = 10;
        }

        // Get overflow handling parameters
        String overflowHandlingMethod = getMediatorParameter(mc, Constants.OVERFLOW_HANDLING_METHOD, String.class, true);
        if (overflowHandlingMethod == null) {
            overflowHandlingMethod = Constants.TRIM; // Default to trim
        }
        
        Object summarizationLlmConfigKeyObj = mc.getProperty(Constants.SUMMARIZATION_LLM_CONFIG_KEY);
        String summarizationLlmConfigKey = summarizationLlmConfigKeyObj != null ? summarizationLlmConfigKeyObj.toString() : null;
        
        String summarizationModelName = getMediatorParameter(mc, Constants.SUMMARIZATION_MODEL_NAME, String.class, true);

        Long toolExecutionTimeout = getMediatorParameter(mc, Constants.TOOL_EXECUTION_TIMEOUT, Long.class, true);
        if (toolExecutionTimeout != null) {
            toolDefinitionsMap.get(agentID).setToolExecutionTimeout(toolExecutionTimeout * 1000);
        }

        String parsedPrompt =
                parseInlineExpression(mc, getMediatorParameter(mc, Constants.PROMPT, String.class, false));
        String attachments = parseInlineExpression(mc,
                getMediatorParameter(mc, Constants.ATTACHMENTS, String.class, true));

        ChatModel model = null;
        try {
            model = LLMConnectionHandler.getChatModel(connectionName, modelName, temperature, maxTokens, topP,
                    frequencyPenalty, seed);
            if (model == null) {
                handleConnectorException(Errors.LLM_CONNECTION_ERROR, mc);
            }
        } catch (Exception e) {
            handleConnectorException(Errors.LLM_CONNECTION_ERROR, mc, e);
        }

        try {
            SynapseLog synLog = getLog(mc);
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Start : AI Agent mediator");

                if (synLog.isTraceTraceEnabled()) {
                    synLog.traceTrace("Message : " + mc.getEnvelope());
                }
            }

            Object memoryConfigKeyObj = mc.getProperty(Constants.MEMORY_CONFIG_KEY);
            String memoryConfigKey = memoryConfigKeyObj != null ? memoryConfigKeyObj.toString() : null;
            String sessionId =
                    getMediatorParameter(mc, Constants.SESSION_ID, String.class, StringUtils.isEmpty(memoryConfigKey));
            if (StringUtils.isEmpty(sessionId)) {
                sessionId = "default";
            }
            
            // Get chat memory with overflow handling support
            Object llmConfigKeyObj = mc.getProperty(Constants.LLM_CONFIG_KEY);
            String llmConfigKey = llmConfigKeyObj != null ? llmConfigKeyObj.toString() : null;
            
            ChatMemory chatMemory = Utils.getChatMemoryWithOverflowHandling(
                    sessionId, 
                    memoryConfigKey, 
                    maxChatHistory,
                    overflowHandlingMethod,
                    summarizationLlmConfigKey,
                    summarizationModelName,
                    llmConfigKey,  // Use agent's LLM as default for summarization
                    modelName      // Use agent's model as default for summarization
            );

            // AiServiceContext constructor is protected in langchain4j 1.9.1+, manage components directly
            AiServiceContext aiServiceContext = null;

            SystemMessage systemMessage = new SystemMessage(system);
            UserMessage userMessage = Utils.buildUserMessage(parsedPrompt, attachments);

            chatMemory.add(systemMessage);
            chatMemory.add(userMessage);

            List<ToolSpecification> toolSpecs = toolDefinitionsMap.get(agentID).getToolSpecifications();
            ChatRequestParameters parameters = ChatRequestParameters.builder()
                    .toolSpecifications(toolSpecs)
//                    .responseFormat(ResponseFormat.JSON) // TODO: add response format support
                    .build();

            int executionsLeft = MAX_TOOL_EXECUTIONS_PER_REQUEST;

            SharedAgentDataHolder sharedAgentDataHolder = new SharedAgentDataHolder();
            sharedAgentDataHolder.setAgentId(agentID);
            sharedAgentDataHolder.setOverwriteBody(overWriteBody);
            sharedAgentDataHolder.setResponseVariable(responseVariable);

            // Clone the original MessageContext and save it to continue the next iteration of agent inference
            MessageContext orginalMessageContext = MessageHelper.cloneMessageContext(mc);
            sharedAgentDataHolder.setSynCtx(orginalMessageContext);
            sharedAgentDataHolder.setAiServiceContext(aiServiceContext);
            sharedAgentDataHolder.setChatMemory(chatMemory);
            sharedAgentDataHolder.setChatModel(model);
            sharedAgentDataHolder.setToolSpecifications(toolSpecs);
            sharedAgentDataHolder.setSystemMessageProvider(chatMemoryId -> system);
            sharedAgentDataHolder.setExecutionsLeft(executionsLeft);
            sharedAgentDataHolder.setChatRequestParameters(parameters);
            sharedAgentDataHolder.setMemoryId(sessionId);
            mc.setProperty(Constants.AGENT_SHARED_DATA_HOLDER + "." + agentID, sharedAgentDataHolder);

            return inferenceAgentAndExecuteTools(mc, synLog, agentID);
        } catch (Exception e) {
            handleConnectorException(Errors.CHAT_COMPLETION_ERROR, mc, e);
        }
        return true;
    }

    private String buildSystemPrompt(String role, String instructions, MessageContext mc) {

        StringBuilder prompt = new StringBuilder();
        if (StringUtils.isNotEmpty(role)) {
            prompt.append("# Role").append(System.lineSeparator())
                    .append(role).append(System.lineSeparator()).append(System.lineSeparator());
        }
        if (StringUtils.isNotEmpty(instructions)) {
            prompt.append("## Instructions").append(System.lineSeparator())
                    .append(parseInlineExpression(mc, instructions));
        }
        if (prompt.length() == 0) {
            return DEFAULT_SYSTEM_PROMPT;
        }
        return prompt.toString();
    }

    private boolean inferenceAgentAndExecuteTools(MessageContext mc, SynapseLog synLog, String agentID) {

        boolean agentInferenceFinished = false;
        boolean toolExecutionResultAggregate = false;
        SharedAgentDataHolder sharedAgentDataHolder =
                (SharedAgentDataHolder) mc.getProperty(Constants.AGENT_SHARED_DATA_HOLDER + "." + agentID);
        String memoryId = sharedAgentDataHolder.getMemoryId();
        ChatMemory chatMemory = sharedAgentDataHolder.getChatMemory();
        ChatModel chatModel = sharedAgentDataHolder.getChatModel();

        List<ChatMessage> messages = chatMemory.messages();
        AgentUtils.addSystemMessageIfMissing(messages, sharedAgentDataHolder.getSystemMessageProvider(), memoryId, DEFAULT_SYSTEM_PROMPT);
        ChatRequest chatRequest =
                ChatRequest.builder().messages(messages).parameters(sharedAgentDataHolder.getChatRequestParameters())
                        .build();

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Submitting chat request to the LLM model");
            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Chat Request : " + chatRequest);
            }
        }

        ChatResponse chatResponse = chatModel.chat(chatRequest);
        sharedAgentDataHolder.setTokenUsageAccumulator(
                TokenUsage.sum(sharedAgentDataHolder.getTokenUsageAccumulator(), chatResponse.metadata().tokenUsage()));

        AiMessage aiMessage = chatResponse.aiMessage();
        sharedAgentDataHolder.addToMemory(aiMessage);

        if (!aiMessage.hasToolExecutionRequests()) {
            agentInferenceFinished = true;
            sharedAgentDataHolder.setFinishChatResponse(chatResponse);
        } else {
            sharedAgentDataHolder.setCurrentToolExecutionRequests(aiMessage.toolExecutionRequests());

            int i = 0;
            int toolExecutionsSize = aiMessage.toolExecutionRequests().size();
            for (ToolExecutionRequest toolExecutionRequest : aiMessage.toolExecutionRequests()) {
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("Submitting " + (i + 1) + " of " + aiMessage.toolExecutionRequests().size() +
                            " messages for parallel tool execution");
                    if (synLog.isTraceTraceEnabled()) {
                        synLog.traceTrace("Tool Execution Request : " + toolExecutionRequest);
                    }
                }

                // Handle hallucinated tool execution requests
                if (!toolDefinitionsMap.get(agentID).getToolInvokers().containsKey(toolExecutionRequest.name())) {
                    addToolExecutionResultToDataHolder(sharedAgentDataHolder, toolExecutionRequest,
                            Constants.HALLUCINATED_TOOL_EXECUTION_REQUEST);
                    continue;
                }

                int executionsLeft = sharedAgentDataHolder.getAndDecrementExecutionsLeft();
                if (executionsLeft == 0) {
                    MessageContext orginalMessageContext = sharedAgentDataHolder.getSynCtx();
                    handleConnectorException(Errors.EXCEEDED_SEQUENTIAL_TOOL_EXECUTIONS, orginalMessageContext);
                }

                MessageContext clonedMessageContext =
                        getClonedMessageContextForToolExecution(mc, agentID, i + 1, toolExecutionsSize);

                // Store the current tool execution request data in the cloned message context
                ToolExecutionDataHolder toolExecutionDataHolder = new ToolExecutionDataHolder();
                toolExecutionDataHolder.setAgentID(agentID);
                toolExecutionDataHolder.setToolExecutionRequest(toolExecutionRequest);
                toolExecutionDataHolder.setTotalToolExecutionCount(aiMessage.toolExecutionRequests().size());
                toolExecutionDataHolder.setCurrentToolExecutionIndex(i++);
                Value resultExpression =
                        toolDefinitionsMap.get(agentID).getToolResultExpression().get(toolExecutionRequest.name());
                toolExecutionDataHolder.setResultExpression(resultExpression);
                clonedMessageContext.setProperty(Constants.TOOL_EXECUTION_DATA_HOLDER + "." + agentID,
                        toolExecutionDataHolder);

                executeTool(agentID, toolExecutionRequest, clonedMessageContext, toolExecutionsSize);
            }
        }
        OperationContext opCtx
                = ((Axis2MessageContext) mc).getAxis2MessageContext().getOperationContext();
        if (opCtx != null) {
            opCtx.setProperty(org.apache.axis2.Constants.RESPONSE_WRITTEN, "SKIP");
        }
        if (agentInferenceFinished) {
            return completeAgentInference(mc, sharedAgentDataHolder);
        }
        return toolExecutionResultAggregate;
    }

    @Override
    public boolean mediate(MessageContext messageContext, ContinuationState continuationState) {

        SynapseLog synLog = getLog(messageContext);
        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("AI Agent: Mediating from continuation state");
        }

        boolean result = false;
        boolean readyToAggregate = false;

        String agentID = getAgentIDFromCallback(messageContext);
        if (agentID == null) {
            handleException("Agent ID not found in the callback", messageContext);
        }
        ToolExecutionDataHolder toolExecutionDataHolder = (ToolExecutionDataHolder) messageContext.getProperty(
                Constants.TOOL_EXECUTION_DATA_HOLDER + "." + agentID);
        if (!continuationState.hasChild()) {
            Object fromMediatorWorker = messageContext.getProperty(
                    SynapseConstants.CONTINUE_FLOW_TRIGGERED_FROM_MEDIATOR_WORKER);
            if (fromMediatorWorker != null && (Boolean) fromMediatorWorker) {
                messageContext.setProperty(SynapseConstants.CONTINUE_FLOW_TRIGGERED_FROM_MEDIATOR_WORKER, false);
            }
            readyToAggregate = true;
        } else {
            synLog.traceOrDebug(
                    "Continuation is triggered from a callback, mediating through the child continuation state");
            ToolExecutionRequest toolExecutionRequest = toolExecutionDataHolder.getToolExecutionRequest();
            SequenceMediator toolMediator = toolDefinitionsMap.get(agentID).getToolInvoker(toolExecutionRequest.name());
            FlowContinuableMediator mediator = (FlowContinuableMediator) toolMediator.getChild(0);
            result = mediator.mediate(messageContext, continuationState.getChildContState());
        }

        if (readyToAggregate) {
            return aggregateToolExecutionResult(toolExecutionDataHolder.getAgentID(), messageContext,
                    toolExecutionDataHolder.getTotalToolExecutionCount(), synLog);
        }
        return result;
    }

    private String getAgentIDFromCallback(MessageContext messageContext) {

        Stack<TemplateContext> funcStack = (Stack) messageContext.getProperty("_SYNAPSE_FUNCTION_STACK");
        String agentId = null;
        if (funcStack != null && !funcStack.isEmpty()) {
            for (int i = funcStack.size() - 1; i >= 0; i--) {
                TemplateContext templateContext = funcStack.get(i);
                if (Constants.AGENT.equals(templateContext.getName())) {
                    agentId = templateContext.getParameterValue(SynapseConstants.INVOKE_MEDIATOR_ID).toString();
                    break;
                }
            }
        }
        return agentId;
    }

    private boolean aggregateToolExecutionResult(String agentID, MessageContext synCtx, int toolCount,
                                                 SynapseLog synLog) {

        ContinuationStackManager.removeReliantContinuationState(synCtx);
        ToolExecutionAggregate aggregate = null;

        String correlationIdName = Constants.TOOL_EXECUTION_CORRELATION + "." + agentID;

        Object correlationID = synCtx.getProperty(correlationIdName);
        String correlation = (String) correlationID;

        synLog.traceOrDebug("Aggregating agent tool execution messages started for correlation : " + correlation);

        Long toolExecutionTimeout = toolDefinitionsMap.get(agentID).getToolExecutionTimeout();

        while (aggregate == null) {
            synchronized (getLock(agentID)) {
                if (activeAggregates.containsKey(agentID) && activeAggregates.get(agentID).containsKey(correlation)) {
                    aggregate = activeAggregates.get(agentID).get(correlation);
                    if (aggregate != null) {
                        if (!aggregate.getLock()) {
                            aggregate = null;
                        }
                    }
                } else {
                    if (synLog.isTraceOrDebugEnabled()) {
                        synLog.traceOrDebug("Creating new Aggregator for agent tool execution - " +
                                (toolExecutionTimeout > 0 ? "expires in : "
                                        + (toolExecutionTimeout / 1000) + "secs" : "without expiry time"));
                    }
                    if (isAggregationCompleted(agentID, synCtx)) {
                        return false;
                    }

                    aggregate = new ToolExecutionAggregate(
                            synCtx.getEnvironment(),
                            correlation,
                            toolExecutionTimeout,
                            toolCount, this, synCtx.getFaultStack().peek());
                    aggregate.setAgentID(agentID);
                    if (toolExecutionTimeout > 0) {
                        synchronized (aggregate) {
                            if (!aggregate.isCompleted()) {
                                try {
                                    log.info("Scheduling Synapse timer for agent tool execution with " +
                                            "correlation : " + correlation);
                                    synCtx.getConfiguration().getSynapseTimer().
                                            schedule(aggregate, toolExecutionTimeout);
                                } catch (IllegalStateException e) {
                                    log.warn("Synapse timer already cancelled. Resetting Synapse timer");
                                    synCtx.getConfiguration().setSynapseTimer(new Timer(true));
                                    synCtx.getConfiguration().getSynapseTimer().
                                            schedule(aggregate, toolExecutionTimeout);
                                }
                            }
                        }
                    }
                    aggregate.getLock();
                    Map<String, ToolExecutionAggregate> agentAggregates = new HashMap<>();
                    agentAggregates.put(correlation, aggregate);
                    activeAggregates.put(agentID, agentAggregates);
                }
            }
        }

        boolean collected = aggregate.addMessage(synCtx);
        if (synLog.isTraceOrDebugEnabled()) {
            if (collected) {
                synLog.traceOrDebug("Collected a message during aggregation");
                if (synLog.isTraceTraceEnabled()) {
                    synLog.traceTrace("Collected message : " + synCtx);
                }
            }
        }
        if (aggregate.isComplete(synLog)) {
            synLog.traceOrDebug("Aggregation completed for agent tool executions");
            return completeAggregate(aggregate);
        } else {
            aggregate.releaseLock();
        }
        return false;
    }

    public boolean completeAggregate(ToolExecutionAggregate aggregate) {

        boolean markedCompletedNow = false;
        boolean wasComplete = aggregate.isCompleted();
        if (wasComplete) {
            return false;
        }
        log.debug("Aggregation completed or timed out");

        // cancel the timer
        synchronized (this) {
            if (!aggregate.isCompleted()) {
                aggregate.cancel();
                aggregate.setCompleted(true);

                MessageContext lastMessage = aggregate.getLastMessage();
                if (lastMessage != null) {
                    Object aggregateTimeoutHolderObj =
                            lastMessage.getProperty(Constants.AGENT_SHARED_DATA_HOLDER + "." + aggregate.getAgentID());

                    if (aggregateTimeoutHolderObj != null) {
                        SharedDataHolder sharedDataHolder = (SharedDataHolder) aggregateTimeoutHolderObj;
                        sharedDataHolder.markAggregationCompletion();
                    }
                } else {
                    aggregate.getSharedAgentDataHolder().markAggregationCompletion();
                }
                markedCompletedNow = true;
            }
        }

        if (!markedCompletedNow) {
            return false;
        }
        MessageContext originalMessageContext = getOriginalMessageContext(aggregate);
        if (originalMessageContext != null) {
            // Clone the message context to preserve the original message context for the next iteration
            MessageContext clonedMessageContext =
                    getClonedMessageContext(originalMessageContext, aggregate.getAgentID());
            if (clonedMessageContext != null) {
                collectToolExecutionResults(clonedMessageContext, aggregate);
                aggregate.clear();
                activeAggregates.get(aggregate.getAgentID()).remove(aggregate.getCorrelation());
                return continueAgentInference(aggregate.getAgentID(), clonedMessageContext);
            } else {
                handleException(aggregate, "Error cloning the original message context", null, originalMessageContext);
                return false;
            }
        } else {
            handleException(aggregate, "Error retrieving the original message context", null,
                    aggregate.getLastMessage());
            return false;
        }
    }

    private boolean continueAgentInference(String agentID, MessageContext originalMessageContext) {

        SharedAgentDataHolder sharedAgentDataHolder = (SharedAgentDataHolder) originalMessageContext.getProperty(
                Constants.AGENT_SHARED_DATA_HOLDER + "." + agentID);
        if (sharedAgentDataHolder != null) {
            sharedAgentDataHolder.resetAggregationCompletion();
            return inferenceAgentAndExecuteTools(originalMessageContext, getLog(originalMessageContext),
                    sharedAgentDataHolder.getAgentId());
        }
        return false;
    }

    private void collectToolExecutionResults(MessageContext originalMessageContext, ToolExecutionAggregate aggregate) {

        log.debug("Collecting aggregated tool executions responses to the original message context");
        if (aggregate.getToolCount() != aggregate.getMessages().size()) {
            log.warn("The agent tool executions are not finished for the correlation : "
                    + aggregate.getCorrelation());
        }
        SharedAgentDataHolder sharedAgentDataHolder = extractToolExecutionResult(aggregate.getAgentID(), aggregate);
        originalMessageContext.setProperty(Constants.AGENT_SHARED_DATA_HOLDER + "." + aggregate.getAgentID(),
                sharedAgentDataHolder);
    }

    private SharedAgentDataHolder extractToolExecutionResult(String agentID, ToolExecutionAggregate aggregate) {

        SharedAgentDataHolder sharedAgentDataHolder;
        if (aggregate.getLastMessage() != null) {
            sharedAgentDataHolder = (SharedAgentDataHolder) aggregate.getLastMessage().getProperty(
                    Constants.AGENT_SHARED_DATA_HOLDER + "." + agentID);
        } else {
            sharedAgentDataHolder = aggregate.getSharedAgentDataHolder();
        }
        List<ToolExecutionRequest> toolExecutionRequestList = new ArrayList<>(
                sharedAgentDataHolder.getCurrentToolExecutionRequests());
        for (MessageContext synCtx : aggregate.getMessages()) {
            ToolExecutionDataHolder toolExecutionDataHolder = (ToolExecutionDataHolder) synCtx.getProperty(
                    Constants.TOOL_EXECUTION_DATA_HOLDER + "." + agentID);
            Value resultVariable = toolExecutionDataHolder.getResultExpression();
            if (resultVariable != null) {
                String result = resultVariable.evaluateValue(synCtx);
                if (StringUtils.isNoneBlank(result)) {
                    addToolExecutionResultToDataHolder(sharedAgentDataHolder, toolExecutionDataHolder
                            .getToolExecutionRequest(), result);
                    toolExecutionRequestList.remove(toolExecutionDataHolder.getToolExecutionRequest());
                }
            }
        }
        if (!toolExecutionRequestList.isEmpty()) {
            for (ToolExecutionRequest toolExecutionRequest : toolExecutionRequestList) {
                log.warn("Tool execution failed or timed out. Marking it as failed. Tool: "
                        + toolExecutionRequest.name());
                addToolExecutionResultToDataHolder(sharedAgentDataHolder, toolExecutionRequest,
                        Constants.TOOL_EXECUTION_FAILED);
            }
        }
        return sharedAgentDataHolder;
    }

    @SuppressWarnings("deprecation")
    private void addToolExecutionResultToDataHolder(SharedAgentDataHolder sharedAgentDataHolder,
                                                    ToolExecutionRequest executionRequest, String result) {

        sharedAgentDataHolder.getToolExecutions()
                .add(ToolExecution.builder().request(executionRequest).result(result).build());
        ToolExecutionResultMessage toolExecutionResultMessage =
                ToolExecutionResultMessage.from(executionRequest, result);
        sharedAgentDataHolder.addToMemory(toolExecutionResultMessage);
    }

    private boolean completeAgentInference(MessageContext mc, SharedAgentDataHolder sharedAgentDataHolder) {

        MessageContext originalMessageContext = getOriginalMessageContext(sharedAgentDataHolder);
        ChatResponse finishChatResponse = sharedAgentDataHolder.getFinishChatResponse();
        Result<Object> parsedResponse =
                parseFinalResponse(originalMessageContext, finishChatResponse, sharedAgentDataHolder);
        if (parsedResponse != null) {
            // Build response object to avoid Gson serialization issues with AtomicReference
            Object responseObject = buildResponseObject(parsedResponse);
            handleConnectorResponse(originalMessageContext, sharedAgentDataHolder.getResponseVariable(),
                    sharedAgentDataHolder.isOverwriteBody(), responseObject, null, null);
        } else {
            handleConnectorException(Errors.INVALID_OUTPUT_TYPE, originalMessageContext);
        }

        ContinuationStackManager.updateSeqContinuationState(originalMessageContext, getMediatorPosition());

        getLog(originalMessageContext).traceOrDebug("End : Agent mediator");

        boolean result;
        do {
            SeqContinuationState seqContinuationState =
                    (SeqContinuationState) ContinuationStackManager.peakContinuationStateStack(originalMessageContext);
            if (seqContinuationState != null) {
                SequenceMediator sequenceMediator =
                        ContinuationStackManager.retrieveSequence(originalMessageContext, seqContinuationState);
                result = sequenceMediator.mediate(originalMessageContext, seqContinuationState);
                if (RuntimeStatisticCollector.isStatisticsEnabled()) {
                    sequenceMediator.reportCloseStatistics(originalMessageContext, null);
                }
            } else {
                break;
            }
        } while (result && !originalMessageContext.getContinuationStateStack().isEmpty());

        return false;
    }

    private void handleException(ToolExecutionAggregate aggregate, String msg, Exception exception,
                                 MessageContext msgContext) {

        aggregate.clear();
        activeAggregates.get(aggregate.getAgentID()).remove(aggregate.getCorrelation());
        if (exception != null) {
            super.handleException(msg, exception, msgContext);
        } else {
            super.handleException(msg, msgContext);
        }
    }

    private MessageContext getOriginalMessageContext(ToolExecutionAggregate aggregate) {

        MessageContext lastMessage = aggregate.getLastMessage();
        if (lastMessage != null) {
            Object aggregateHolderObj =
                    lastMessage.getProperty(Constants.AGENT_SHARED_DATA_HOLDER + "." + aggregate.getAgentID());
            return getOriginalMessageContext((SharedAgentDataHolder) aggregateHolderObj);
        } else if (aggregate.getSharedAgentDataHolder() != null) {
            return getOriginalMessageContext(aggregate.getSharedAgentDataHolder());
        }
        return null;
    }

    private MessageContext getOriginalMessageContext(SharedAgentDataHolder sharedAgentDataHolder) {

        if (sharedAgentDataHolder != null) {
            return sharedAgentDataHolder.getSynCtx();
        }
        return null;
    }

    private boolean isAggregationCompleted(String agentID, MessageContext synCtx) {

        Object aggregateTimeoutHolderObj = synCtx.getProperty(Constants.AGENT_SHARED_DATA_HOLDER + "." + agentID);

        if (aggregateTimeoutHolderObj != null) {
            SharedDataHolder sharedDataHolder = (SharedDataHolder) aggregateTimeoutHolderObj;
            if (sharedDataHolder.isAggregationCompleted()) {
                log.debug("Received a response for already completed Aggregate");
                return true;
            }
        }
        return false;
    }

    private MessageContext getClonedMessageContextForToolExecution(MessageContext mc, String agentID, int toolId,
                                                                   int totalToolExecutions) {

        MessageContext newCtx = getClonedMessageContext(mc, agentID);

        // Set isServerSide property in the cloned message context
        ((Axis2MessageContext) newCtx).getAxis2MessageContext().setServerSide(
                ((Axis2MessageContext) mc).getAxis2MessageContext().isServerSide());

        // Set the SCATTER_MESSAGES property in the cloned message context to continue from MediatorWorker
        newCtx.setProperty(SynapseConstants.SCATTER_MESSAGES, true);
        newCtx.setProperty(Constants.AGENT_TOOL_EXECUTION + "." + agentID,
                toolId + EIPConstants.MESSAGE_SEQUENCE_DELEMITER + totalToolExecutions);
        return newCtx;
    }

    private MessageContext getClonedMessageContext(MessageContext mc, String agentID) {

        MessageContext newCtx = null;
        try {
            newCtx = MessageHelper.cloneMessageContext(mc);
            newCtx.setProperty(Constants.TOOL_EXECUTION_CORRELATION + "." + agentID, mc.getMessageID());
        } catch (AxisFault axisFault) {
            handleException("Error cloning the message context", axisFault, mc);
        }
        return newCtx;
    }

    private void executeTool(String agentID, ToolExecutionRequest toolExecutionRequest, MessageContext mc,
                             int toolCount) {

        ContinuationStackManager.addReliantContinuationState(mc, 0, getMediatorPosition());
        SequenceMediator invoker =
                toolDefinitionsMap.get(agentID).getToolInvoker(toolExecutionRequest.name());
        Map<String, Object> arguments = AgentUtils.argumentsAsMap(toolExecutionRequest.arguments());

        // Set the tool execution request arguments as properties to invoke the sequence template
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            EIPUtils.createSynapseEIPTemplateProperty(mc, toolExecutionRequest.name(), entry.getKey(),
                    entry.getValue());
        }

        if (log.isDebugEnabled()) {
            log.debug("Asynchronously mediating using the tool invoker anonymous sequence");
        }

        String correlationIdName = Constants.TOOL_EXECUTION_CORRELATION + "." + agentID;
        String correlation = (String) mc.getProperty(correlationIdName);
        if (!activeAggregates.containsKey(agentID) || !activeAggregates.get(agentID).containsKey(correlation)) {
            startToolExecutionTimeout(agentID, mc, correlation, toolCount);
        }
        mc.getEnvironment().injectAsync(mc, invoker);
    }

    private void startToolExecutionTimeout(String agentID, MessageContext mc, String correlation, int toolCount) {

        SharedAgentDataHolder sharedAgentDataHolder = (SharedAgentDataHolder) mc.getProperty(
                Constants.AGENT_SHARED_DATA_HOLDER + "." + agentID);
        Long toolExecutionTimeout = toolDefinitionsMap.get(agentID).getToolExecutionTimeout();
        ToolExecutionAggregate aggregate =
                new ToolExecutionAggregate(mc.getEnvironment(), correlation, toolExecutionTimeout, toolCount,
                        this, mc.getFaultStack().peek(), sharedAgentDataHolder);
        aggregate.setAgentID(agentID);
        try {
            log.info("Scheduling Synapse timer for agent tool execution with correlation : " +
                    correlation);
            mc.getConfiguration().getSynapseTimer().
                    schedule(aggregate, toolExecutionTimeout);
        } catch (IllegalStateException e) {
            log.warn("Synapse timer already cancelled. Resetting Synapse timer");
            mc.getConfiguration().setSynapseTimer(new Timer(true));
            mc.getConfiguration().getSynapseTimer().
                    schedule(aggregate, toolExecutionTimeout);
        }
        if (activeAggregates.containsKey(agentID)) {
            activeAggregates.get(agentID).put(correlation, aggregate);
        } else {
            Map<String, ToolExecutionAggregate> aggregateMap = new HashMap<>();
            aggregateMap.put(correlation, aggregate);
            activeAggregates.put(agentID, aggregateMap);
        }
    }

    private Result<Object> parseFinalResponse(MessageContext mc, ChatResponse chatResponse,
                                              SharedAgentDataHolder agentDataHolder) {

        TokenUsage tokenUsageAccumulator = agentDataHolder.getTokenUsageAccumulator();
        FinishReason finishReason = chatResponse.metadata().finishReason();

        // TODO: Support different output types such as int, boolean, Json(as schema), etc.
        Object parsedResponse = serviceOutputParser.parse(chatResponse, String.class);
        Result<Object> parsedResult = Result.builder()
                .content(parsedResponse)
                .tokenUsage(tokenUsageAccumulator)
                .finishReason(finishReason)
                .toolExecutions(agentDataHolder.getToolExecutions())
                .build();
        return parsedResult;
    }

    private java.util.Map<String, Object> buildResponseObject(Result<?> result) {
        // Use LinkedHashMap to preserve field order matching version 0.1.8
        java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
        
        // Field order: content -> tokenUsage -> finishReason -> toolExecutions
        response.put("content", result.content());
        
        // Add token usage information
        if (result.tokenUsage() != null) {
            java.util.Map<String, Object> tokenUsage = new java.util.LinkedHashMap<>();
            tokenUsage.put("cacheCreationInputTokens", 0); // Default values for backward compatibility
            tokenUsage.put("cacheReadInputTokens", 0);
            tokenUsage.put("inputTokenCount", result.tokenUsage().inputTokenCount());
            tokenUsage.put("outputTokenCount", result.tokenUsage().outputTokenCount());
            tokenUsage.put("totalTokenCount", result.tokenUsage().totalTokenCount());
            
            response.put("tokenUsage", tokenUsage);
        }
        
        // Add finish reason
        if (result.finishReason() != null) {
            response.put("finishReason", result.finishReason().toString());
        }
        
        // Add tool executions
        response.put("toolExecutions", result.toolExecutions() != null ? result.toolExecutions() : new java.util.ArrayList<>());
        
        return response;
    }

    @Override
    public void execute(MessageContext messageContext, String responseVariable, Boolean overwriteBody) {
        // This method is not needed as we override the mediate method
    }
}
