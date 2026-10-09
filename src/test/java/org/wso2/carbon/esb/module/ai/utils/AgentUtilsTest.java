package org.wso2.carbon.esb.module.ai.utils;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class AgentUtilsTest {

    @Test
    public void testEvictionWithOrphanedAiMessage() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new AiMessage("Previous orphaned response"));
        messages.add(new UserMessage("New prompt"));

        AgentUtils.addSystemMessageIfMissing(messages, null, "test-session", "You are helpful.");

        Assert.assertEquals(messages.size(), 2);
        Assert.assertTrue(messages.get(0) instanceof SystemMessage);
        Assert.assertTrue(messages.get(1) instanceof UserMessage);
        Assert.assertEquals(((UserMessage) messages.get(1)).singleText(), "New prompt");
    }

    @Test
    public void testEvictionWithOrphanedToolExecutionResult() {
        List<ChatMessage> messages = new ArrayList<>();
        ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
                .id("call_123")
                .name("get_weather")
                .arguments("{}")
                .build();
        messages.add(ToolExecutionResultMessage.from(toolRequest, "Sunny"));
        messages.add(new AiMessage("Weather is sunny"));
        messages.add(new UserMessage("Follow-up question"));

        AgentUtils.addSystemMessageIfMissing(messages, null, "test-session", "You are helpful.");

        Assert.assertEquals(messages.size(), 2);
        Assert.assertTrue(messages.get(0) instanceof SystemMessage);
        Assert.assertTrue(messages.get(1) instanceof UserMessage);
        Assert.assertEquals(((UserMessage) messages.get(1)).singleText(), "Follow-up question");
    }

    @Test
    public void testValidConversationRemainsUntouched() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage("You are helpful."));
        messages.add(new UserMessage("Hello"));
        messages.add(new AiMessage("Hi there!"));

        AgentUtils.addSystemMessageIfMissing(messages, null, "test-session", "You are helpful.");

        Assert.assertEquals(messages.size(), 3);
        Assert.assertTrue(messages.get(0) instanceof SystemMessage);
        Assert.assertTrue(messages.get(1) instanceof UserMessage);
        Assert.assertTrue(messages.get(2) instanceof AiMessage);
    }
}