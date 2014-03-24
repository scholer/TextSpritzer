package net.clintarmstrong.textspritzer.lib;

import android.util.Log;

/**
 * Created by carmstrong on 3/20/14.
 */
public class DefaultWordStrategy implements WordStrategy {
    protected static final int MAX_WORD_LENGTH = 13;
    protected static final boolean VERBOSE = false;
    protected static final String TAG = "DefaultWordStrategy";

    @Override
    public WordObj parseWord(String input){
        WordObj retWordObj = new WordObj();
        
        // Split first word from string, return remainingwords
        if (VERBOSE) Log.d(TAG, "Splitting off first word.");
        String[] wordArray = WordUtils.getNextWord(input);

        String word = wordArray[0];
        if (VERBOSE) Log.d(TAG, "word is: " + word);
        retWordObj.remainingWords = wordArray[1];
        if (VERBOSE) Log.d(TAG, "Remaining words are: " + wordArray[1]);


        // Split long words
        if (word.length() > MAX_WORD_LENGTH) {
            String[] longWordArr = WordUtils.splitLongWord(word, MAX_WORD_LENGTH);
            word = longWordArr[0];
            retWordObj.remainingWords = retWordObj.remainingWords + longWordArr[1] + " ";
        }

        // Set Delay for punctuation and length
        if (word.length() >= 6 || word.contains(",") || word.contains(":") || word.contains(";") || word.contains(".") || word.contains("?") || word.contains("!") || word.contains("\"")) {
            retWordObj.delayMultiplier = 3;
        } else {
            retWordObj.delayMultiplier = 1;
        }

        retWordObj.parsedWord = word;

        return retWordObj;
    }
}
