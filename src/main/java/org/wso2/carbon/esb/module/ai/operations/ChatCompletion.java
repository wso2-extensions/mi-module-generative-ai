/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
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
package org.wso2.carbon.esb.module.ai.operations;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;

interface Agent {
    String chat(String userMessage);
}

public class ChatCompletion extends AbstractConnector {

    String SYSTEM_PROMPT_NAME = "systemPrompt";
    String PROMPT_NAME = "prompt";
    String OUTPUT_NAME = "output";
    String API_KEY = "ai_openai_apiKey";

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        String systemPromptName = (String) getParameter(messageContext, SYSTEM_PROMPT_NAME);
        String promptName = (String) getParameter(messageContext, PROMPT_NAME);
        String output = (String) getParameter(messageContext, OUTPUT_NAME);

        String apiKey = (String) messageContext.getProperty(API_KEY);
        String systemPrompt = (String) messageContext.getProperty(systemPromptName);
        String prompt = (String) messageContext.getProperty(promptName);
        try {
            OpenAiChatModel model = OpenAiChatModel.withApiKey(apiKey);
            Agent agent = AiServices.builder(Agent.class)
                    .chatLanguageModel(model)
                    .systemMessageProvider(chatMemoryId -> {
                        if (systemPrompt == null) {
                            return "You are a helpful assistant.";
                        }
                        return systemPrompt;
                    })
                    .build();
            String answer = agent.chat(prompt);
            messageContext.setProperty(output, answer);
        } catch (Exception e) {
            log.error(e);
	        throw new ConnectException(e);
        }
    }
}
