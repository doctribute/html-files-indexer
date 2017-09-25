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
package org.doctribute.html.indexer.model;

public class WordInfo {

    private final String word;
    private int scoring;

    public WordInfo(String word, int scoring) {
        this.word = word;
        this.scoring = scoring;
    }

    public String getWord() {
        return word;
    }

    public void setScoring(int scoring) {
        this.scoring = scoring;
    }

    public int getScoring() {
        return scoring;
    }

}
