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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.doctribute.html.indexer.model.ContentInfo;
import org.doctribute.html.indexer.model.FileInfo;
import org.doctribute.html.indexer.model.WordInfo;
import org.tartarus.snowball.SnowballProgram;

public class SaxHtmlFileIndexer extends SaxHtmlFileParser {

    private final static int SCORING_FOR_H1 = 50;
    private final static int SCORING_FOR_H2 = 45;
    private final static int SCORING_FOR_H3 = 40;
    private final static int SCORING_FOR_H4 = 35;
    private final static int SCORING_FOR_H5 = 30;
    private final static int SCORING_FOR_H6 = 25;
    private final static int SCORING_FOR_BOLD = 5;
    private final static int SCORING_FOR_ITALIC = 3;
    private final static int SCORING_FOR_NORMAL_TEXT = 1;

    private final SnowballProgram stemmer;
    private final String stopwordsRegexPattern;
    private final String punctuationRegexPattern;
    private final Map<String, String> indicesMap;
    private int i = 0;

    public SaxHtmlFileIndexer(SnowballProgram stemmer, String stopwordsRegexPattern, String punctuationRegexPattern, Map<String, String> indicesMap) {

        super();

        // prefer TagSoup parser to enable processing even invalid HTML files
        System.setProperty("org.xml.sax.driver", "org.ccil.cowan.tagsoup.Parser");
        System.setProperty("javax.xml.parsers.SAXParserFactory", "org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl");

        this.stemmer = stemmer;
        this.stopwordsRegexPattern = stopwordsRegexPattern;
        this.punctuationRegexPattern = punctuationRegexPattern;
        this.indicesMap = indicesMap;
    }

    @Override
    public void extractData(Path path, FileInfo fileInfo, String contentIDRegexPattern) throws IOException {

        this.fileInfo = fileInfo;
        this.contentIDRegexPattern = contentIDRegexPattern;

        contentInfoList = new ArrayList<>();

        parseDocument(path);

        Map<String, WordInfo> wordInfoMap = new HashMap<>();

        for (ContentInfo contentInfo : contentInfoList) {

            int scoring = getScoring(contentInfo.getElementName());

            String text = contentInfo.getContent().toLowerCase();
            text = text.replaceAll(punctuationRegexPattern, " ");
            text = text.replaceAll(stopwordsRegexPattern, "");

            List<String> wordList = new ArrayList<>(Arrays.asList(text.split("\\s+")));
            List<String> derivedWordList = new ArrayList<>();
            List<String> obsoleteWordList = new ArrayList<>();

            for (String word : wordList) {

                // keep derived words unique per single word
                Collection<String> derivedWordCollection = new HashSet<>();

                word = getWordWithoutEnclosedPunctuation(word, obsoleteWordList, derivedWordCollection);

                String[] chunks = word.split("[\\.:-]");

                if (chunks.length > 1) {
                    for (String chunk : chunks) {
                        if (!chunk.isEmpty()) {
                            derivedWordCollection.add(chunk);
                        }
                    }
                }

                derivedWordList.addAll(derivedWordCollection);
            }

            wordList.addAll(derivedWordList);
            wordList.removeAll(obsoleteWordList);

            for (String word : wordList) {

                if (!word.isEmpty()) {

                    String stemWord = getStemWord(word);

                    if (wordInfoMap.containsKey(stemWord)) {
                        WordInfo wordInfo = wordInfoMap.get(stemWord);
                        wordInfo.setScoring(wordInfo.getScoring() + scoring);

                    } else {
                        wordInfoMap.put(stemWord, new WordInfo(word, scoring));
                    }
                }
            }
        }

        for (Entry<String, WordInfo> entry : wordInfoMap.entrySet()) {

            String stemWord = entry.getKey();
            String indice = i + "*" + entry.getValue().getScoring();

            if (indicesMap.containsKey(stemWord)) {
                indice = indicesMap.get(stemWord) + "," + indice;
            }

            indicesMap.put(stemWord, indice);
        }

        i++;
    }

    private String getWordWithoutEnclosedPunctuation(String word, List<String> obsoleteWordList, Collection<String> derivedWordCollection) {

        if (word.matches("^[\\.:-]+.*") || word.matches(".*[\\.:-]+$")) {

            obsoleteWordList.add(word);

            while (word.matches("^[\\.:-]+.*") || word.matches(".*[\\.:-]+$")) {
                word = word.replaceAll("^[\\.:-]+(.*)", "$1");
                word = word.replaceAll("(.*)[\\.:-]+$", "$1");
            }

            derivedWordCollection.add(word);
        }

        return word;
    }

    private String getStemWord(String word) {

        stemmer.setCurrent(word.trim().toLowerCase());
        stemmer.stem();

        return stemmer.getCurrent();
    }

    private int getScoring(String elementName) {

        int scoring = SCORING_FOR_NORMAL_TEXT;

        switch (elementName.toLowerCase()) {
            case "h1":
                scoring = SCORING_FOR_H1;
                break;
            case "h2":
                scoring = SCORING_FOR_H2;
                break;
            case "h3":
                scoring = SCORING_FOR_H3;
                break;
            case "h4":
                scoring = SCORING_FOR_H4;
                break;
            case "h5":
                scoring = SCORING_FOR_H5;
                break;
            case "h6":
                scoring = SCORING_FOR_H6;
                break;
            case "i":
            case "em":
                scoring = SCORING_FOR_ITALIC;
                break;
            case "b":
            case "strong":
                scoring = SCORING_FOR_BOLD;
                break;
        }

        return scoring;
    }

}
