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

package org.wso2.carbon.esb.module.ai.operations;

import org.apache.synapse.MessageContext;
import org.apache.synapse.util.InlineExpressionUtil;
import org.jaxen.JaxenException;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;

/**
 * Prompt mediator
 * @author Isuru Wijesiri
 */
public class Prompt extends AbstractAIMediator {

    @Override
    public void initialize(MessageContext mc) {}

    @Override
    public void execute(MessageContext mc) {
        String prompt = getMediatorParameter(mc, "prompt", String.class, false);
        String responseVariable = getMediatorParameter(mc, "responseVariable", String.class, false);
        Boolean overwriteBody = getMediatorParameter(mc, "overwriteBody", Boolean.class, false);

        try {
            String parsedPrompt = InlineExpressionUtil.processInLineSynapseExpressionTemplate(mc, prompt);
            handleResponse(mc, responseVariable, overwriteBody, parsedPrompt, null, null);
        } catch (JaxenException e) {
            handleException("Failed to parse prompt", e, mc);
        }
        // mc.setProperty(responseVariable, prompt);
    }
}
