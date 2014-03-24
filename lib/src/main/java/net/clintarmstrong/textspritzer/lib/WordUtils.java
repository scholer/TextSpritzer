package net.clintarmstrong.textspritzer.lib;

import android.util.Log;

/**
 * Created by carmstrong on 3/22/14.
 */
public class WordUtils {
    public static final String TAG = "WORDUTILS";
//  protected static final boolean VERBOSE = (BuildConfig.DEBUG);
    protected static final boolean VERBOSE = true;

    public static String[] getNextWord(String input){
        return input
                .replaceAll("/\\s+/g", " ")      // condense adjacent spaces
                .split(" ", 2);
    }

    public static int findSplitIndex(String thisWord, int maxWordLength) {
        int splitIndex;
        // Split long words, at hyphen or dot if present.
        if (thisWord.contains("-")) {
            splitIndex = thisWord.indexOf("-") + 1;
        } else if (thisWord.contains(".")) {
            splitIndex = thisWord.indexOf(".") + 1;
        } else if (thisWord.length() > maxWordLength * 2) {
            // if the word is floccinaucinihilipilifcation, for example.
            splitIndex = maxWordLength - 1;
            // 12 characters plus a "-" == 13.
        } else {
            // otherwise we want to split near te middle.
            splitIndex = Math.round(thisWord.length() / 2F);
        }
        // in case we found a split character that was > MAX_WORD_LENGTH characters in.
        if (splitIndex > maxWordLength) {
            // If we split the word at a splitting char like "-" or ".", we added one to the splitIndex
            // in order to ensure the splitting char appears at the head of the split. Not accounting
            // for this in the recursive call will cause a StackOverflowException
            return findSplitIndex(thisWord.substring(0,
                    wordContainsSplittingCharacter(thisWord) ? splitIndex - 1 : splitIndex), maxWordLength);
        }
        if (VERBOSE) {
            Log.i(TAG, "Splitting long word " + thisWord + " into " + thisWord.substring(0, splitIndex) +
                    " and " + thisWord.substring(splitIndex));
        }
        return splitIndex;
    }

    public static boolean wordContainsSplittingCharacter(String word) {
        return (word.contains(".") || word.contains("-"));
    }

    // Split long words, and return array
    public static String[] splitLongWord(String word, int maxWordLength) {
        int splitIndex = findSplitIndex(word, maxWordLength);
        String firstSegment;
        if (VERBOSE) {
            Log.i(TAG, "Splitting long word " + word + " into " + word.substring(0, splitIndex) + " and " + word.substring(splitIndex));
        }
        firstSegment = word.substring(0, splitIndex);
        // A word split is always indicated with a hyphen unless ending in a period
        if (!firstSegment.contains("-") && !firstSegment.endsWith(".")) {
            firstSegment = firstSegment + "-";
        }
        String[] retArray = {firstSegment, word.substring(splitIndex)};
        return retArray;
    }
}
