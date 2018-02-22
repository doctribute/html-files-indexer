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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.doctribute.html.indexer.model.FileInfo;
import org.doctribute.html.indexer.util.StopwordsParser;
import org.doctribute.html.indexer.util.PunctuationParser;
import org.tartarus.snowball.SnowballProgram;

public class Indexer {

    private static final String PARAM_SOURCE_FOLDER_PATH = "-sourceFolderPath";
    private static final String PARAM_STEMMER_CLASS_NAME = "-stemmerClassName";
    private static final String PARAM_CONTENT_IDS = "-contentIDs";
    private static final String PARAM_STOPWORDS_FILE_PATH = "-stopwordsFilePath";
    private static final String PARAM_PUNCTUATION_FILE_PATH = "-punctuationFilePath";

    private static final String OUTPUT_FOLDER_NAME = "search";
    private static final String FILE_INFO_LIST_NAME = "file-info-list.js";
    private static final String DEFAULT_CONTENT_ID_REGEX_PATTERN = "content";
    private static final String DEFAULT_PUNCTUATION_REGEX_PATTERN = "[$\\|%,;'()\\\\/*\"{}=!&+<>#‚’‘”“´…\\?\\u00A0]|\\[|\\]|[-][-]+";

    public static void main(String[] args) throws IOException {

        Map<String, String> passedValuesMap = new HashMap<>();

        for (String arg : args) {
            int index = arg.indexOf(":");
            if (index > 0 && index < arg.length() - 1) {
                passedValuesMap.put(arg.substring(0, index), arg.substring(index + 1));
            }
        }

        if (passedValuesMap.containsKey(PARAM_SOURCE_FOLDER_PATH) && passedValuesMap.containsKey(PARAM_STEMMER_CLASS_NAME)) {

            SnowballProgram stemmer = null;

            try {
                Class stemmerClass = Class.forName(passedValuesMap.get(PARAM_STEMMER_CLASS_NAME));
                stemmer = (SnowballProgram) stemmerClass.newInstance();

            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                throw new IOException("The stemmer couldn't be initialized.", e);
            }

            Path sourceFolderPath = Paths.get(passedValuesMap.get(PARAM_SOURCE_FOLDER_PATH));

            String contentIDRegexPattern = DEFAULT_CONTENT_ID_REGEX_PATTERN;

            if (passedValuesMap.containsKey(PARAM_CONTENT_IDS)) {
                contentIDRegexPattern = getContentIDRegexPattern(passedValuesMap.get(PARAM_CONTENT_IDS));
            }

            String stopwordsRegexPattern = "";

            if (passedValuesMap.containsKey(PARAM_STOPWORDS_FILE_PATH)) {
                stopwordsRegexPattern = StopwordsParser.getStopwordsRegexPattern(Paths.get(passedValuesMap.get(PARAM_STOPWORDS_FILE_PATH)));
            }

            String punctuationRegexPattern = DEFAULT_PUNCTUATION_REGEX_PATTERN;

            if (passedValuesMap.containsKey(PARAM_PUNCTUATION_FILE_PATH)) {
                punctuationRegexPattern = PunctuationParser.getPunctuationRegexPattern(Paths.get(passedValuesMap.get(PARAM_PUNCTUATION_FILE_PATH)));
            }

            execute(sourceFolderPath, contentIDRegexPattern, stemmer, stopwordsRegexPattern, punctuationRegexPattern);

        } else {

            System.out.println("Specify at least:\n"
                    + "- the directory containing html files (sourceFolderPath)\n"
                    + "- the stemmer class name (stemmerClassName)\n\n"
                    + "Usage: java -jar indexer.jar \n"
                    + "         -sourceFolderPath:output/html \n"
                    + "         -stemmerClassName:org.tartarus.snowball.ext.EnglishStemmer \n"
                    + "        [-contentIDs:header-content,body-content] \n"
                    + "        [-stopwordsFilePath:search/stopwords.js] \n"
                    + "        [-punctuationFilePath:search/punctuation.js]"
            );
        }
    }

    public static void execute(Path sourceFolderPath, String contentIDRegexPattern, SnowballProgram stemmer, String stopwordsRegexPattern, String punctuationRegexPattern) throws IOException {

        Collection htmlPathCollection = getHtmlPathCollection(sourceFolderPath);

        if (htmlPathCollection.isEmpty()) {
            return;
        }

        List<Path> htmlPathList = new ArrayList<>(htmlPathCollection);
        Collections.sort(htmlPathList);

        Map<String, String> indicesMap = new HashMap<>();

        SaxHtmlFileIndexer indexer = new SaxHtmlFileIndexer(stemmer, stopwordsRegexPattern, punctuationRegexPattern, indicesMap);

        Map<Path, FileInfo> fileInfoMap = new LinkedHashMap<>();

        for (Path htmlPath : htmlPathList) {
            FileInfo fileInfo = new FileInfo();
            indexer.extractData(htmlPath, fileInfo, contentIDRegexPattern);
            fileInfoMap.put(htmlPath, fileInfo);
        }

        if (!indicesMap.isEmpty()) {

            Path outputFolderPath = sourceFolderPath.resolve(OUTPUT_FOLDER_NAME);

            if (Files.notExists(outputFolderPath)) {
                Files.createDirectories(outputFolderPath);
            }

            writeFileInfoList(sourceFolderPath, outputFolderPath.resolve(FILE_INFO_LIST_NAME), fileInfoMap);
            writeIndices(indicesMap, outputFolderPath);
        }
    }

    private static String getContentIDRegexPattern(String delimitedContentIDs) {

        String contentIDRegexPattern = "";

        if (delimitedContentIDs != null) {

            String[] contentIDs = delimitedContentIDs.split(",");

            for (String contentID : contentIDs) {
                contentIDRegexPattern += contentID + "|";
            }
        }

        return contentIDRegexPattern;
    }

    private static Collection<Path> getHtmlPathCollection(Path sourceFolderPath) throws IOException {

        Collection<Path> htmlPathColection = new HashSet<>();

        Files.walkFileTree(sourceFolderPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                if (path.toString().endsWith(".html")) {
                    htmlPathColection.add(path);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return htmlPathColection;
    }

    private static void writeFileInfoList(Path sourceFolderPath, Path outputFilePath, Map<Path, FileInfo> fileInfoMap) throws IOException {

        try (BufferedWriter writer = Files.newBufferedWriter(outputFilePath)) {

            writer.write("fil = new Array();\n");

            int i = 0;
            for (Entry<Path, FileInfo> entry : fileInfoMap.entrySet()) {

                String title = entry.getValue().getTitle();

                if (title != null) {
                    title = title.replaceAll("\\s+", " ");
                    title = title.replaceAll("['�\"]", " ");
                    title = title.replaceAll("\\\\", "\\\\\\\\");
                }
                writer.write("fil[\"" + i + "\"] = \"" + sourceFolderPath.relativize(entry.getKey()).toString().replace("\\", "/") + "@@@" + title + "\";\n");
                i++;
            }
        }
    }

    private static void writeIndices(Map<String, String> indicesMap, Path outputFolderPath) throws IOException {

        List<String> keyList = new ArrayList(indicesMap.keySet());
        Collections.sort(keyList);

        int size = 1 + keyList.size() / 3;

        for (int i = 0; i < 3; i++) {

            try (BufferedWriter writer = Files.newBufferedWriter(outputFolderPath.resolve("index-" + (i + 1) + ".js"))) {

                int upperBound = Math.min(keyList.size(), i * size + size);

                for (int j = i * size; j < upperBound; j++) {
                    String key = keyList.get(j);
                    writer.write("w[\"" + key + "\"]" + "=\"" + indicesMap.get(key) + "\";\n");
                }
            }
        }
    }
}
