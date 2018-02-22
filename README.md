# HTML Files Indexer

## Motivation

When it comes to search, online documentation provided as a collection of static HTML pages can't usually compete with the capabilities of decent Content Management System (CMS).

To enable searching in static HTML pages it is necessary to provide
 1. a search index (kind of database of all the keywords with additional metadata)
 2. client-side code which handles querying the search index and processing the results

Both parts are available for DocBook WebHelp output in the official XSL Stylesheets distribution. The search index is created by a tailor made indexer which extracts all the available text content from the particular HTML page, splits the words, normalizes them and stores this info together with the calculated weight into a text file. In this file (search index) the result of all the HTML pages is aggregated so every keyword is unique, but linked to one or more HTML pages. When a search keyword is entered into the search field in the frontend, it is normalized and then queried in the search index file. When it is found, all HTML pages where this keyword has been detected during indexing are returned. The results are ordered by the weight.

For my use case I found the indexer part somewhat limited:
 1. No stemming support for various European languages
 2. Missing support for 'stop' words (removing dummy words can reduce the final search index file size)

These issues have been covered. Additionally, the entire code has been refactored to be more readable and maintainable.

## How to build

 - Clone this repository to your local disc.
 - Ensure that JDK 8 is available on your system.
 - Open this Maven based project in your favorite IDE.
 - Build the project.

The final jar file is located in the `target` subfolder. All dependencies are copied in the nested `lib` subfolder.

## How to use

For usage just run the tool in console without any parameters:
`java -jar html-files-indexer-{version}.jar`

If default stemmers are sufficient, just run the command above with additional parameters.

In case of custom stemmer we have to alter the class path, so the command has to reflect this:
`java -cp lib/tagsoup-1.2.1.jar;lib/lucene-analyzers-common-7.0.1.jar;lib/lucene-core-7.0.1.jar;html-files-indexer.jar;custom-stemmer.jar org.doctribute.html.indexer.Indexer `

## Limitations

The support for CJK languages has been removed.


## Adding a custom stemmer

### Creating Java class from SBL file (cygwin)
 1. Fork `https://github.com/snowballstem/snowball` rev 502
 2. Create new subfolder in the `algorithms` folder and copy there the given SBL file renamed to `stem_Unicode.sbl`
 3. Add stemmer configuration into `libstemmer/modules.txt` and `libstemmer/modules_utf8.txt`
 4. Add stemmer to the GNUmakefile's `libstemmer_algorithms` variable
 5. Fix the header of `libstemmer/mkmodules.pl` to `#!/usr/bin/env perl`
 6. Compile the Snowball using `make dist`

### Modify Java class to be compatible with Lucene library
 1. Edit the result file:

### Create JavaScript files
 1. Fork `https://github.com/mazko/jssnowball`
 2. Install rsync (via cygwin)
 3. Install esjava and babel (via npm)
 4. `cd jssnowball-master`
 5. Create bundle containing all java sources via `make bundle`
 6. Edit bundle to be compatible with ESJava (manually edit code with es6 comments, remove package names in method references (org.tartarus.snowball, java.lang), remove overloaded methods, finnishStemmer.class -> es6bridge)
 7. Generate JavaScript via `make esjava`
 8. Generate es5 using babel manually
 9. npm install -save-dev babel-cli
10. npm install --save-dev babel-plugin-transform-es2015-modules-umd
11. npm install --save-dev babel-preset-es2015
12. npx babel --compact=false --presets es2015 --plugins transform-es2015-modules-umd --module-id snowballFactory js_snowball/lib/snowball.es6 --out-file js_snowball/lib/snowball.babel.js

var _typeof = typeof Symbol === "function" && typeof Symbol.iterator === "symbol" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol ? "symbol" : typeof obj; };
