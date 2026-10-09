/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.esb.module.ai.config;

import org.apache.synapse.MessageContext;
import org.wso2.carbon.connector.core.AbstractConnector;

/**
 * Checks if an error was set by a previous operation and propagates it.
 * This is used to surface errors that were swallowed by shorthand filter blocks in Synapse templates.
 * The error response is already set on the response variable by handleConnectorException,
 * so this class just needs to halt the mediation flow.
 */
public class ErrorChecker extends AbstractConnector {

    private static final String AI_ERROR_MESSAGE = "_AI_ERROR_MESSAGE";

    @Override
    public void connect(MessageContext messageContext) {

        Object errorMessage = messageContext.getVariable(AI_ERROR_MESSAGE);
        if (errorMessage != null && !errorMessage.toString().isEmpty()) {
            String error = errorMessage.toString();
            // Clear the error variable so it doesn't trigger again
            messageContext.setVariable(AI_ERROR_MESSAGE, null);
            handleException(error, messageContext);
        }
    }
}
