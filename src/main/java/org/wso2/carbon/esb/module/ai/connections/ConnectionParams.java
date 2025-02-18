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

package org.wso2.carbon.esb.module.ai.connections;

import org.apache.synapse.SynapseException;

import java.util.HashMap;

public class ConnectionParams {

    String connectionName;
    String connectionType;
    HashMap<String, String> connectionProperties;

    public ConnectionParams(
            String connectionName, String connectionType, HashMap<String, String> connectionProperties) {
        this.connectionName = connectionName;
        this.connectionType = connectionType;
        this.connectionProperties = connectionProperties;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public String getConnectionProperty(String key) {
        String property = connectionProperties.get(key);
        if (property == null) {
            throw new SynapseException("Property " + key + " is not set for connection " + connectionName);
        }
        return property;
    }
}
