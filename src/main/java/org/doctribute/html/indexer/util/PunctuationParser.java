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
package org.doctribute.html.indexer.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PunctuationParser {

    public static String getPunctuationRegexPattern(Path path) throws IOException {

        String punctuationRegexPattern = null;

        try (BufferedReader reader = Files.newBufferedReader(path)) {

            String line;

            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty() && line.contains("\"")) {
                    int start = line.indexOf("\"") + 1;
                    int end = line.lastIndexOf("\"");
                    if (start < end) {
                        punctuationRegexPattern = line.substring(start, end).replace("\\\\", "\\");
                        break;
                    }
                }
            }
        }

        return punctuationRegexPattern;
    }
}
