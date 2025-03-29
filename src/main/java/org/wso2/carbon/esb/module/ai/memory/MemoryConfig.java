/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.esb.module.ai.memory;

import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.esb.module.ai.Constants;
import org.wso2.carbon.esb.module.ai.config.ConnectorUndeployObserver;
import org.wso2.carbon.esb.module.ai.database.PostgresDatabase;
import org.wso2.carbon.esb.module.ai.database.RDBMSDatabase;
import org.wso2.carbon.esb.module.ai.memory.store.MemoryStoreHandler;
import org.wso2.carbon.esb.module.ai.memory.store.RDBMSChatMemoryStore;

public class MemoryConfig extends AbstractConnector implements ManagedLifecycle {

    @Override
    public void init(SynapseEnvironment synapseEnvironment) {

        SynapseConfiguration synapseConfig = synapseEnvironment.getSynapseConfiguration();
        synapseConfig.registerObserver(new ConnectorUndeployObserver(synapseConfig));
    }

    @Override
    public void destroy() {

    }

    @Override
    public void connect(MessageContext messageContext) {

        String connectionName = (String) getParameter(messageContext, Constants.NAME);
        String connectionType = (String) getParameter(messageContext, Constants.CONNECTION_TYPE);

        String host = (String) getParameter(messageContext, Constants.HOST);
        String port = (String) getParameter(messageContext, Constants.PORT);
        String database = (String) getParameter(messageContext, Constants.DATABASE);
        String user = (String) getParameter(messageContext, Constants.USER);
        String password = (String) getParameter(messageContext, Constants.PASSWORD);
        String table = (String) getParameter(messageContext, Constants.TABLE);

        MemoryStoreHandler memoryStoreHandler = MemoryStoreHandler.getMemoryStoreHandler();

        if (MemoryType.POSTGRES_MEMORY.name().equals(connectionType)) {
            RDBMSDatabase.Builder builder = new PostgresDatabase.Builder();
            builder.host(host).port(port).database(database).user(user).password(password).table(table);
            memoryStoreHandler.addMemoryStore(connectionName, new RDBMSChatMemoryStore(builder.build()));
        } else if (MemoryType.IN_MEMORY.name().equals(connectionType)) {
            memoryStoreHandler.addMemoryStore(connectionName, new InMemoryChatMemoryStore());
        } else {
            handleException("Unsupported memory type: " + connectionType, messageContext);
        }
    }
}
