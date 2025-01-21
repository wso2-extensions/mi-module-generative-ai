package org.wso2.carbon.esb.module.ai.operations;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


public class HTMLToTextParser implements DocumentParser {
    @Override
    public Document parse(InputStream inputStream) {
        org.jsoup.nodes.Document jsoupDocument;

        try {
            // Parse the input stream with UTF-8 encoding
            jsoupDocument = Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), "");
        } catch (IOException e) {
            throw new RuntimeException("Error parsing HTML input stream", e);
        }

        return jsoupDocument.body() != null ? new Document(jsoupDocument.body().text()) : null;
    }
}
