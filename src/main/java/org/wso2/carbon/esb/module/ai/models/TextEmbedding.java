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

package org.wso2.carbon.esb.module.ai.models;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.langchain4j.data.document.Metadata;

public class TextEmbedding {
    private String text;
    private float[] embedding;
    private final Metadata metadata;

    public TextEmbedding(String text, float[] embedding, Metadata metadata) {
        this.text = text;
        this.embedding = embedding;
        this.metadata = metadata;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public JsonElement serialize() {
        var jsonObject = new JsonObject();
        jsonObject.addProperty("text", text);
        jsonObject.add("embedding", new Gson().toJsonTree(embedding));
        jsonObject.add("metadata", new Gson().toJsonTree(metadata.toMap()));
        return jsonObject;
    }
}
