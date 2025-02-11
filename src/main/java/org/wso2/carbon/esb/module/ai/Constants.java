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

package org.wso2.carbon.esb.module.ai;

public class Constants {
    // Content types
    public final static String JSON_CONTENT_TYPE = "application/json";

    // Vector store connection properties
    public static final String CONNECTION_TYPE = "connectionType";
    public static final String CONNECTION_NAME = "connectionName";
    public static final String PERSISTENCE = "persistence";
    public static final String URL = "url";
    public static final String COLLECTION = "collection";
    public static final String API_KEY = "apiKey";
    public static final String CLOUD = "cloud";
    public static final String REGION = "region";
    public static final String INDEX = "index";
    public static final String NAMESPACE = "namespace";
    public static final String DIMENSION = "dimension";
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String DATABASE = "database";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String TABLE = "table";

    // LLM connection properties
    public static final String DEPLOYMENT_NAME = "deploymentName";
    public static final String ENDPOINT = "endpoint";

    // Parser Constants
    public static final String MD_TO_TEXT = "markdown-to-text";
    public static final String HTML_TO_TEXT = "html-to-text";
    public static final String PDF_TO_TEXT = "pdf-to-text";
    public static final String DOC_TO_TEXT = "doc-to-text";
    public static final String DOCX_TO_TEXT = "docx-to-text";
    public static final String PPT_TO_TEXT = "ppt-to-text";
    public static final String PPTX_TO_TEXT = "pptx-to-text";
    public static final String XLS_TO_TEXT = "xls-to-text";
    public static final String XLSX_TO_TEXT = "xlsx-to-text";

    public static final String INPUT = "input";
    public static final String TYPE = "type";

    // Output Constants
    public static final String RESPONSE_VARIABLE = "responseVariable";
    public static final String OVERWRITE_BODY = "overwriteBody";
}
