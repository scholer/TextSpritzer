package net.clintarmstrong.textspritzer.lib;

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

        // Split first word from string. wordArray[0] will be the separated word we want to process, wordArray[1] is the remaining string.
        String[] wordArray = WordUtils.getNextWord(input);

        // if there is no text remaining, set callback to 1 so that this thread can be notified.
        // Useful for adding more text at regular intervals.
        if (wordArray.length > 1) {
            retWordObj.remainingWords = wordArray[1];
        } else {
            retWordObj.callback = 1;
        }
        String word = wordArray[0];
        int delayMultiplier = 1;

        // Split long words
        if (word.length() > MAX_WORD_LENGTH) {
            String[] longWordArr = WordUtils.splitLongWord(word, MAX_WORD_LENGTH);
            word = longWordArr[0];
            retWordObj.remainingWords = retWordObj.remainingWords + longWordArr[1] + " ";
        }

        // set delay 3x for words with punctuation, 1x for all others. Does not run if delaymultiplier is already set from metadata above.
        if (delayMultiplier == 1){
            if (word.length() >= 6 || word.contains(",") || word.contains(":") || word.contains(";") || word.contains(".") || word.contains("?") || word.contains("!") || word.contains("\"")) {
                // Set Delay for punctuation and length
                delayMultiplier = 2;
            } else {
                delayMultiplier = 1;
            }
        }

        retWordObj.parsedWord = word;
        retWordObj.delayMultiplier = delayMultiplier;

        return retWordObj;
    }
}
