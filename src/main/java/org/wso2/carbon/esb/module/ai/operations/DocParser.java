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
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;

import java.io.ByteArrayInputStream;
import java.util.Base64;

/**
 * Parsing operation
 *
 * Inputs:
 * - input: String
 * - parserType: Type of the parser
 * - responseVariable: Variable name to store the output
 *
 * Outputs:
 * - Parsed text
 */
public class DocParser extends AbstractAIMediator {

    // Text content types
    private static final String TEXT_TYPE = "text/plain";
    private static final String JSON_TYPE = "application/json";
    private static final String XML_TYPE = "application/xml";
    private static final String HTML_TYPE = "text/html";
    private static final String MARKDOWN_TYPE = "text/markdown";

    // PDF content type
    private static final String PDF_TYPE = "application/pdf";

    // Document content types
    private static final String DOC_TYPE = "application/msword";
    private static final String DOCX_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String PPT_TYPE = "application/vnd.ms-powerpoint";
    private static final String PPTX_TYPE = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    private static final String XLS_TYPE = "application/vnd.ms-excel";
    private static final String XLSX_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    enum PARSER {
        TEXT,
        PDF_BOX,
        POI,
        HTML_TO_TEXT
    }

    @Override
    public void initialize(MessageContext mc) {}

    @Override
    public void execute(MessageContext mc) {
        String input = getMediatorParameter(mc, "input", String.class, false);
        String parserType = getMediatorParameter(mc, "type", String.class, false);
        String responseVariable = getMediatorParameter(mc, "responseVariable", String.class, false);

        PARSER parser = null;
        parser = parserType.equalsIgnoreCase("auto") ? autoDetectParser(mc) : determineParser(parserType);

        input = input.equalsIgnoreCase("payload") ? mc.getEnvelope().getBody().getFirstElement().getText() : input;

        DocumentParser docParser = null;
        ByteArrayInputStream inputStream = null;
        switch (parser) {
            case TEXT:
                docParser = new TextDocumentParser();
                inputStream = new ByteArrayInputStream(input.getBytes());
                break;
            case HTML_TO_TEXT:
                docParser = new HTMLToTextParser();
                inputStream = new ByteArrayInputStream(input.getBytes());
                break;
            case PDF_BOX:
                docParser = new ApachePdfBoxDocumentParser();
                inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(input));
                break;
            case POI:
                docParser = new ApachePoiDocumentParser();
                inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(input));
                break;
            default:
                handleException("Unsupported content type: " + parserType, mc);
        }

        Document doc= docParser.parse(inputStream);
        if (doc == null) {
            handleException("Error parsing document", mc);
        }

        handleResponse(mc, responseVariable, doc.text(), null, null);
    }

    private PARSER determineParser(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "markdown-to-text" -> PARSER.TEXT;
            case "html-to-text"-> PARSER.HTML_TO_TEXT;
            case "pdf-to-text" -> PARSER.PDF_BOX;
            case "doc-to-text", "docx-to-text", "ppt-to-text", "pptx-to-txt", "xls-to-text", "xlsx-to-text" -> PARSER.POI;
            default -> null;
        };
    }

    private PARSER autoDetectParser(MessageContext mc) {
        Object payloadType = ((Axis2MessageContext) mc).getAxis2MessageContext().getProperty("ContentType");
        if (payloadType == null) {
            handleException("Content type cannot be determined", mc);
        }
        return switch (payloadType.toString().toLowerCase()) {
            case TEXT_TYPE, MARKDOWN_TYPE -> PARSER.TEXT;
            case HTML_TYPE -> PARSER.HTML_TO_TEXT;
            case PDF_TYPE -> PARSER.PDF_BOX;
            case DOC_TYPE, DOCX_TYPE, PPT_TYPE, PPTX_TYPE, XLS_TYPE, XLSX_TYPE -> PARSER.POI;
            default -> null;
        };
    }
}
