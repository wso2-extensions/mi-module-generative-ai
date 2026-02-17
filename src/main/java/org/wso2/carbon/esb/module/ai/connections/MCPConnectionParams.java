/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

import java.util.HashMap;

/**
 * MCP Connection Parameters
 * Extends ConnectionParams to include headers for MCP server authentication
 */
public class MCPConnectionParams extends ConnectionParams {

    private HashMap<String, String> headers;

    public MCPConnectionParams(String connectionName, String connectionType, 
                              HashMap<String, String> connectionProperties,
                              HashMap<String, String> headers) {
        super(connectionName, connectionType, connectionProperties);
        this.headers = headers != null ? headers : new HashMap<>();
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public String getHeader(String headerName) {
        return headers.get(headerName);
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public boolean hasHeaders() {
        return !headers.isEmpty();
    }
}
