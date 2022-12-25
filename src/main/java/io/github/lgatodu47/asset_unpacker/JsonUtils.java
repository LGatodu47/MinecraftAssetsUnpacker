package io.github.lgatodu47.asset_unpacker;

import java.util.Map;

/**
 * Utility class for Json things.
 */
class JsonUtils {
    /**
     * Reads the content of an asset index file and associates the paths of each asset with its hash.
     *
     * @param content The content of the indexes file.
     * @param pathToHash The map in which the assets info will be stored.
     */
    static void readIndexesJson(String content, Map<String, String> pathToHash) {
        int level = 0;
        boolean readingStr = false;
        StringBuilder sb = new StringBuilder();
        String assetPath = null;
        boolean hashNext = false;

        for(int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            // starting from second level there is the info we want to get
            if(level > 1) {
                if(readingStr) {
                    // Logic for handling the string that was read
                    if(c == '"') {
                        String str = sb.toString();
                        // There are only assets paths at level 2
                        if(level == 2) {
                            assetPath = str;
                        } else if(str.equals("hash")) {
                            // The next string we will read will probably be a hash
                            hashNext = true;
                        } else if(hashNext) {
                            // The string we read is a hash
                            hashNext = false;
                            if(assetPath != null) {
                                // We bind the hash we read to the path we also read earlier.
                                pathToHash.put(assetPath, str);
                                assetPath = null;
                            }
                        }
                        // Reset the builder
                        sb = new StringBuilder();
                        readingStr = false;
                    } else {
                        // We write the chars to the builder while the string is not over
                        sb.append(c);
                    }
                    continue;
                }

                // We start reading a string
                if(c == '"') {
                    readingStr = true;
                }
            }

            if(c == '{') {
                level++;
            }
            if(c == '}') {
                level--;
            }
        }
    }
}
