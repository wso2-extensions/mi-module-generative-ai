package org.wso2.carbon.esb.module.ai.operations;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import org.jsoup.Jsoup;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class HTMLToMDParser implements DocumentParser {
    @Override
    public Document parse(InputStream inputStream) {
        org.jsoup.nodes.Document jsoupDocument;
        String markdown = null;
        try {
            // Parse the input stream with UTF-8 encoding
            jsoupDocument = Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), "");
            markdown = FlexmarkHtmlConverter.builder().build().convert(jsoupDocument.html());
        } catch (IOException e) {
            throw new RuntimeException("Error parsing HTML input stream", e);
        }

        return markdown != null ? new Document(markdown) : null;
    }
}
