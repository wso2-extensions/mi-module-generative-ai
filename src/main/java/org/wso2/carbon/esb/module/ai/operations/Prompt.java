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

import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;

/**
 * Prompt mediator
 * @author Isuru Wijesiri
 */
public class Prompt extends AbstractAIMediator {

    String NAME = "name";
    String PROMPT = "prompt";

    @Override
    public void execute(MessageContext mc){

        // Load mediator configurations
        String promptName = getMediatorParameter(mc, NAME, String.class, false);
        String prompt = getMediatorParameter(mc, PROMPT, String.class, false);

        mc.setProperty(promptName, prompt);
    }
}
