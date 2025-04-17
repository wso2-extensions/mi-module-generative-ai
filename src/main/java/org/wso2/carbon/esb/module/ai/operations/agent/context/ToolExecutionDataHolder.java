package org.wso2.carbon.esb.module.ai.operations.agent.context;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.apache.synapse.mediators.Value;

public class ToolExecutionDataHolder {

    private String agentID;

    // Current tool execution request
    private ToolExecutionRequest toolExecutionRequest;
    private Value resultExpression;
    private int totalToolExecutionCount;
    private int currentToolExecutionIndex;

    public ToolExecutionRequest getToolExecutionRequest() {

        return toolExecutionRequest;
    }

    public void setToolExecutionRequest(ToolExecutionRequest toolExecutionRequest) {

        this.toolExecutionRequest = toolExecutionRequest;
    }

    public Value getResultExpression() {

        return resultExpression;
    }

    public void setResultExpression(Value resultExpression) {

        this.resultExpression = resultExpression;
    }

    public int getTotalToolExecutionCount() {

        return totalToolExecutionCount;
    }

    public void setTotalToolExecutionCount(int totalToolExecutionCount) {

        this.totalToolExecutionCount = totalToolExecutionCount;
    }

    public int getCurrentToolExecutionIndex() {

        return currentToolExecutionIndex;
    }

    public void setCurrentToolExecutionIndex(int currentToolExecutionIndex) {

        this.currentToolExecutionIndex = currentToolExecutionIndex;
    }

    public String getAgentID() {

        return agentID;
    }

    public void setAgentID(String agentID) {

        this.agentID = agentID;
    }
}
