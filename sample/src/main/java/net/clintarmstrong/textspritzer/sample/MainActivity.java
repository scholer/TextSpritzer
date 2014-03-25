package net.clintarmstrong.textspritzer.sample;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import net.clintarmstrong.textspritzer.lib.Spritzer;
import net.clintarmstrong.textspritzer.lib.SpritzerTextView;
import net.clintarmstrong.textspritzer.lib.WordObj;
import net.clintarmstrong.textspritzer.lib.WordStrategy;
import net.clintarmstrong.textspritzer.lib.WordUtils;

public class MainActivity extends ActionBarActivity {

    public static final String TAG = MainActivity.class.getName();
    // protected static final boolean VERBOSE = (BuildConfig.DEBUG);
    protected static final boolean VERBOSE = true;
    private SpritzerTextView mSpritzerTextView;
    private SeekBar mSeekBarTextSize;
    private SeekBar mSeekBarWpm;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Review the view and set text to be spritzed
        mSpritzerTextView = (SpritzerTextView) findViewById(R.id.spritzTV);
        mSpritzerTextView.setSpritzText("OpenSpritz has nothing to do with Spritz Incorporated. " +
                "This is an open source, community created project, made with love//spritzerpausex5 because Spritz is " +
                "such an awesome technique for reading.");


        //This attaches a progress bar that show exactly how far you are into your spritz
        mProgressBar = (ProgressBar) findViewById(R.id.spritz_progress);
        mSpritzerTextView.attachProgressBar(mProgressBar);
        findViewById(R.id.spritzagain).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSpritzerTextView.setSpritzText("OpenSpritz has nothing to do with Spritz Incorporated. " +
                                "This is an open source, community created project, made with love//spritzerpausex5 because Spritz is " +
                                "such an awesome technique for reading.");
                    }
                }
        );

        findViewById(R.id.AddText).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSpritzerTextView.addSpritzText("OpenSpritz has nothing to do with Spritz Incorporated. " +
                                "This is an open source, community created project, made with love//spritzerpausex5 because Spritz is " +
                                "such an awesome technique for reading.");
                    }
                }
        );


        //Set how fast the spritzer should go
        mSpritzerTextView.setWpm(500);

        //Set Click Control listeners, these will be called when the user uses the click controls
        mSpritzerTextView.setOnClickControlListener(new SpritzerTextView.OnClickControlListener() {
            @Override
            public void onPause() {
                Toast.makeText(MainActivity.this, "Spritzer has been paused", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onPlay() {
                Toast.makeText(MainActivity.this, "Spritzer is playing", Toast.LENGTH_SHORT).show();

            }
        });

        mSpritzerTextView.setRemainingWordsListener(new Spritzer.RemainingWordsListener() {
            @Override
            public void onWordsRemaining() {
                Toast.makeText(MainActivity.this, "5 words remaining in Spritzer.", Toast.LENGTH_SHORT).show();

            }
        });

        mSpritzerTextView.setWordsRemainingToTriggerListener(5);

        mSpritzerTextView.setCallbackListener(new Spritzer.CallBackListener() {
            @Override
            public void onCallBackListener(int callback) {
                switch (callback) {
                    case 1:
                        Toast.makeText(MainActivity.this, "Added secion of text has finished.", Toast.LENGTH_SHORT).show();
                }
            }
        });


        mSpritzerTextView.setWordStrategy(new WordStrategy() {
            // import the default word strategy so we can use some of it's useful methods
            protected static final int MAX_WORD_LENGTH = 13;

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

                // this section of code makes '//spritzerpausex' metadata followed by a number indicating a custom delay multiplier for this metadata.
                if (word.contains("//spritzerpausex")) {
                    // Set delay from text if we find //spritzerpause in the text
                    if (VERBOSE) Log.d(TAG, "About to split word: " + word);
                    String[] splitWord = word.split("//spritzerpausex");
                    word = splitWord[0];
                    delayMultiplier = Integer.parseInt(splitWord[1]);
                    retWordObj.delayMultiplier = Integer.parseInt(splitWord[1]);
                } else {
                    retWordObj.parsedWord = word;
                }

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
        });


        setupSeekBars();

    }

    /**
     * This is just shows two seek bars to change wpm and text size
     */
    private void setupSeekBars() {
        mSeekBarTextSize = (SeekBar) findViewById(R.id.seekBarTextSize);
        mSeekBarWpm = (SeekBar) findViewById(R.id.seekBarWpm);
        if (mSeekBarWpm != null && mSeekBarTextSize != null) {
            mSeekBarWpm.setMax(mSpritzerTextView.getWpm() * 2);

            mSeekBarTextSize.setMax((int) mSpritzerTextView.getTextSize() * 2);
            mSeekBarWpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (progress > 0) {
                        mSpritzerTextView.setWpm(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            mSeekBarTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mSpritzerTextView.setTextSize(progress);

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            mSeekBarWpm.setProgress(mSpritzerTextView.getWpm());
            mSeekBarTextSize.setProgress((int) mSpritzerTextView.getTextSize());
        }

    }


}
