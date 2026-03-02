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

package org.wso2.carbon.esb.module.ai.stores;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore;
import io.weaviate.client.Config;
import io.weaviate.client.WeaviateAuthClient;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.schema.model.Schema;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.esb.module.ai.Errors;
import org.wso2.carbon.esb.module.ai.models.TextEmbedding;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class Weaviate extends VectorStore {
    
    private static final Log log = LogFactory.getLog(Weaviate.class);
    
    private final WeaviateClient directClient;
    private final String objectClass;
    private final boolean avoidDups;
    private final String consistencyLevel;
    private final String baseUrl;
    private final String apiKey;
    
    public Weaviate(String scheme, String host, Integer port, String apiKey, String objectClass, boolean avoidDups, String consistencyLevel) {
        super(buildStore(scheme, host, port, apiKey, normalizeObjectClass(objectClass), avoidDups, consistencyLevel));
        
        this.objectClass = normalizeObjectClass(objectClass);
        this.avoidDups = avoidDups;
        this.consistencyLevel = consistencyLevel;
        this.apiKey = apiKey;
        
        if ("https".equalsIgnoreCase(scheme) && (port == null || port == 443)) {
            this.baseUrl = scheme + "://" + host + "/v1";
        } else if ("http".equalsIgnoreCase(scheme) && (port == null || port == 80)) {
            this.baseUrl = scheme + "://" + host + "/v1";
        } else if (port != null && port > 0) {
            this.baseUrl = scheme + "://" + host + ":" + port + "/v1";
        } else {
            this.baseUrl = scheme + "://" + host + "/v1";
        }
        
        this.directClient = createClient(scheme, host, port, apiKey);
        validateConnection(this.directClient, this.objectClass);
    }
    
    /**
     * Uses direct HTTP REST calls to Weaviate instead of the v5 Java client,
     * because Gson's @SerializedName("class") annotation is not honoured in
     * the OSGi environment, causing the "class" field to serialize as "className".
     */
    @Override
    public void add(List<TextEmbedding> textEmbeddings) {
        int successCount = 0;
        int errorCount = 0;
        
        for (TextEmbedding textEmbedding : textEmbeddings) {
            try {
                String id = avoidDups ? 
                    generateUUIDFrom(textEmbedding.getText()) : 
                    java.util.UUID.randomUUID().toString();
                
                JsonObject jsonBody = new JsonObject();
                jsonBody.addProperty("class", objectClass);
                jsonBody.addProperty("id", id);
                
                JsonObject propsJson = new JsonObject();
                propsJson.addProperty("text", textEmbedding.getText());
                
                if (textEmbedding.getMetadata() != null && !textEmbedding.getMetadata().toMap().isEmpty()) {
                    for (Map.Entry<String, Object> entry : textEmbedding.getMetadata().toMap().entrySet()) {
                        String value = entry.getValue() != null ? entry.getValue().toString() : "";
                        propsJson.addProperty("meta_" + entry.getKey(), value);
                    }
                }
                jsonBody.add("properties", propsJson);
                
                float[] embedding = textEmbedding.getEmbedding();
                JsonArray vectorArray = new JsonArray();
                for (float v : embedding) {
                    vectorArray.add(v);
                }
                jsonBody.add("vector", vectorArray);
                
                httpPost(baseUrl + "/objects", jsonBody.toString());
                successCount++;
                
            } catch (Exception e) {
                errorCount++;
                log.error("Failed to insert embedding: " + e.getMessage(), e);
            }
        }
        
        if (errorCount > 0 && successCount == 0) {
            throw new RuntimeException("All " + errorCount + " Weaviate inserts failed. Check logs for details.");
        } else if (errorCount > 0) {
            log.warn(errorCount + " out of " + textEmbeddings.size() + " inserts failed");
        }
    }
    
    private String httpPost(String urlStr, String jsonBody) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            
            if (apiKey != null && !apiKey.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            
            // Write request body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = conn.getResponseCode();
            
            // Read response body
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                        responseCode >= 200 && responseCode < 300 
                            ? conn.getInputStream() 
                            : conn.getErrorStream(),
                        StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
            
            if (responseCode >= 200 && responseCode < 300) {
                return response.toString();
            } else {
                throw new RuntimeException("Weaviate REST API error (HTTP " + responseCode + "): " + response);
            }
        } finally {
            conn.disconnect();
        }
    }
    
    private static String generateUUIDFrom(String text) {
        return java.util.UUID.nameUUIDFromBytes(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
    }
    
    private static String normalizeObjectClass(String objectClass) {
        if (objectClass == null || objectClass.trim().isEmpty()) {
            return "Default";
        }
        return objectClass.trim();
    }
    
    private static WeaviateClient createClient(String scheme, String host, Integer port, String apiKey) {
        try {
            Config config;
            if ("https".equalsIgnoreCase(scheme) && (port == null || port == 443)) {
                config = new Config(scheme, host);
            } else if ("http".equalsIgnoreCase(scheme) && (port == null || port == 80)) {
                config = new Config(scheme, host);
            } else if (port != null && port > 0) {
                config = new Config(scheme, host + ":" + port);
            } else {
                config = new Config(scheme, host);
            }
            
            if (apiKey != null && !apiKey.isEmpty()) {
                return WeaviateAuthClient.apiKey(config, apiKey);
            } else {
                return new WeaviateClient(config);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Weaviate client: " + e.getMessage(), e);
        }
    }
    
    private static void validateConnection(WeaviateClient client, String objectClass) {
        try {
            Result<Schema> result = client.schema().getter().run();
            
            if (result.hasErrors()) {
                throw new RuntimeException(Errors.WEAVIATE_CONNECTION_ERROR + ": " + result.getError().getMessages());
            }
            
            if (objectClass != null && !objectClass.isEmpty() && result.getResult() != null 
                && result.getResult().getClasses() != null) {
                boolean classExists = result.getResult().getClasses().stream()
                    .anyMatch(cls -> objectClass.equals(cls.getClassName()));
                
                if (!classExists) {
                    log.info("Weaviate class '" + objectClass + "' not found. It will be auto-created on first insertion.");
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(Errors.WEAVIATE_CONNECTION_ERROR + ": " + e.getMessage(), e);
        }
    }
    
    private static WeaviateEmbeddingStore buildStore(String scheme, String host, Integer port, String apiKey, String objectClass, boolean avoidDups, String consistencyLevel) {
        var builder = WeaviateEmbeddingStore.builder()
                .scheme(scheme)
                .host(host)
                .avoidDups(avoidDups)
                .objectClass(objectClass);
        
        // Skip default ports to avoid issues with Weaviate Cloud instances
        if (port != null && port > 0) {
            if (!("https".equalsIgnoreCase(scheme) && port == 443) 
                && !("http".equalsIgnoreCase(scheme) && port == 80)) {
                builder = builder.port(port);
            }
        }
        
        if (apiKey != null && !apiKey.isEmpty()) {
            builder = builder.apiKey(apiKey);
        }
        
        if (consistencyLevel != null && !consistencyLevel.isEmpty()) {
            builder = builder.consistencyLevel(consistencyLevel);
        }
        
        return builder.build();
    }
}
