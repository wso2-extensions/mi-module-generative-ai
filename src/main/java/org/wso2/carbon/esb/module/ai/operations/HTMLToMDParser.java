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

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import org.apache.synapse.SynapseException;
import org.jsoup.Jsoup;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import org.wso2.carbon.esb.module.ai.Errors;
import org.wso2.carbon.esb.module.ai.exceptions.ParsingException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class HTMLToMDParser implements DocumentParser {
    @Override
    public Document parse(InputStream inputStream) throws SynapseException {
        org.jsoup.nodes.Document jsoupDocument;
        String markdown = null;
        try {
            // Parse the input stream with UTF-8 encoding
            jsoupDocument = Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), "");
            markdown = FlexmarkHtmlConverter.builder().build().convert(jsoupDocument.html());
        } catch (IOException e) {
            throw new ParsingException(Errors.HTML_TO_MARKDOWN_ERROR, e);
        }

        return markdown != null ? Document.from(markdown) : null;
    }
}
