/*
 * Copyright 2016-present doctribute (http://doctribute.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 * Jan Tosovsky
 */
package org.doctribute.html.indexer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.doctribute.html.indexer.model.ContentInfo;
import org.doctribute.html.indexer.model.FileInfo;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SaxHtmlFileParser extends DefaultHandler {

    protected FileInfo fileInfo;
    protected List<ContentInfo> contentInfoList;
    protected String contentIDRegexPattern;

    private final Stack<String> elementStack = new Stack<>();
    private boolean addContent = false;
    private int divCount = 0;

    public void extractData(Path path, FileInfo fileInfo, String contentIDRegexPattern) throws IOException {

        this.fileInfo = fileInfo;
        this.contentIDRegexPattern = contentIDRegexPattern;

        contentInfoList = new ArrayList<>();

        parseDocument(path);
    }

    public void parseDocument(Path path) throws IOException {

        SAXParserFactory parserFactory = SAXParserFactory.newInstance();

        addContent = false;
        divCount = 0;

        try {
            SAXParser parser = parserFactory.newSAXParser();

            try (InputStream input = Files.newInputStream(path)) {
                InputSource inputSource = new InputSource(input);
                inputSource.setEncoding("UTF-8");
                parser.parse(inputSource, this);
            }

        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        elementStack.add(qName);

        if ((qName.equalsIgnoreCase("meta"))) {
            String attrName = attributes.getValue("name");
            if (attrName != null && (attrName.equalsIgnoreCase("Section-Title"))) {
                fileInfo.setTitle(attributes.getValue("content").replaceAll("\\s+", " ").trim());
            }
        }

        String id = attributes.getValue("id");
        if (id != null && (id.matches(contentIDRegexPattern))) {
            addContent = true;
        }

        if (qName.equalsIgnoreCase("div") && addContent) {
            divCount++;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        elementStack.pop();

        if (qName.equalsIgnoreCase("div") && addContent) {
            divCount--;
            if (divCount == 0) {
                addContent = false;
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {

        if (addContent) {

            String text = new String(ch, start, length).replaceAll("\\s+", " ").trim();

            if (!text.isEmpty()) {
                contentInfoList.add(new ContentInfo(text, elementStack.peek()));
            }
        }
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        return null;
    }
}
