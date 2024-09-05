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
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;

interface Agent {
    String chat(String userMessage);
}

/**
 * Chat completion mediator
 * @author Isuru Wijesiri
 */
public class ChatCompletion extends AbstractAIMediator {

    String TEMPLATE_SYSTEM_PROMPT = "systemPrompt";
    String TEMPLATE_PROMPT = "prompt";
    String TEMPLATE_OUTPUT_NAME = "output";

    String API_KEY = "ai_openai_apiKey";

    String DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant.";

    @Override
    public void execute(MessageContext mc) {

        // Load mediator configurations from template
        String systemPromptName = getMediatorParameter(mc, TEMPLATE_SYSTEM_PROMPT, String.class, false);
        String promptName = getMediatorParameter(mc, TEMPLATE_PROMPT, String.class, false);
        String output = getMediatorParameter(mc, TEMPLATE_OUTPUT_NAME, String.class, false);

        // Load properties from message context
        String apiKey = getProperty(mc, API_KEY, String.class, false);
        String systemPrompt = getProperty(mc, systemPromptName, String.class, false);
        String prompt = getProperty(mc, promptName, String.class, false);

        try {
            OpenAiChatModel model = OpenAiChatModel.withApiKey(apiKey);
            Agent agent = AiServices.builder(Agent.class)
                    .chatLanguageModel(model)
                    .systemMessageProvider(chatMemoryId -> systemPrompt != null ? systemPrompt : DEFAULT_SYSTEM_PROMPT)
                    .build();
            String answer = agent.chat(prompt);
            mc.setProperty(output, answer);
        } catch (Exception e) {
            log.error(e);
	        handleException("Error while LLM chat completion", e, mc);
        }
    }
}
