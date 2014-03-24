package net.clintarmstrong.textspritzer.lib;

import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Spritzer parses a String into a Queue
 * of words, and displays them one-by-one
 * onto a TextView at a given WPM.
 */
public class Spritzer {

    protected static final boolean VERBOSE = false;
    protected static final String TAG = "Spritzer";

    protected static final int MSG_PRINT_WORD = 1;
    protected static final int CHARS_LEFT_OF_PIVOT = 3;

    protected ArrayDeque<WordObj> mWordQueue; // The queue of word objects yet to be displayed
    protected int mBaseDelay;
    protected boolean mPlaying;
    protected boolean mPlayingRequested;
//    protected boolean mSpritzThreadStarted;
    private WordStrategy mWordStrategy;    // WordStrategy, a class that parses a word and returns a word and int delay
    protected int mCurWordIdx;             // Current progress through mWordQueue, used for moving progress bar
    protected void init() {
        mWordStrategy = new DefaultWordStrategy();
        mWordQueue = new ArrayDeque<WordObj>();
        if (VERBOSE) Log.d(TAG, "Setting inital mBaseDelay to: " + 60000 / 500);
        mBaseDelay = 60000 / 500;
        mPlaying = false;
        mPlayingRequested = false;
//        mSpritzThreadStarted = false;
        mCurWordIdx = 0;
    }

    public void setWordStrategy(WordStrategy strategy) {
        mWordStrategy = strategy;
    }

    public void setText(String input) throws InterruptedException {  // set the spritzer text, removes any existing text in the queue.
        boolean startAgain = false;
        if (mPlayingRequested) startAgain = true;
        if (VERBOSE) Log.d(TAG, "Setting text to: " + input);
        emptyWordQueue();
        addText(input);
        if (startAgain && !mPlayingRequested) start();
        processNextWord();
    }

//    private Thread addWordsThread;          // A separate thread which is parsing words and adding them to the mWordQueue. Available so other threads can interrupt it.
//    private boolean addWordsThreadRunning;  // returns if the thread is still running
//    private Object addWordsThreadSync = new Object();
    private Object newWordAddedSync = new Object();
//    private ThreadPoolExecutor addWordsExecutor = (ThreadPoolExecutor) Executors.newSingleThreadExecutor();
    private ExecutorService addWordsExecutor = Executors.newSingleThreadExecutor();
    private int addWordsExecutorQueueSize = 0;
    public void addText(final String input) {     // Add text to the queue. Continues in a separate thread. Sets addWordsThreadRunning to true while running.
        if (VERBOSE) Log.d(TAG, "Adding Text: " + input);
        addWordsExecutorQueueSize++;
        if (VERBOSE) Log.d(TAG, "addWordsExecutorQueueSize =  " + addWordsExecutorQueueSize);
        addWordsExecutor.submit(new Runnable() {
            public void run() {
                String mInput = input;
                while (mInput != null && !addWordsExecutor.isShutdown() && !addWordsExecutor.isTerminated()) {
                    if (VERBOSE) Log.d(TAG, "Adding remaining text: " + mInput);
                    WordObj mWordObj = mWordStrategy.parseWord(mInput);
                    mInput = mWordObj.remainingWords;
                    mWordObj.remainingWords = null;
                    mWordQueue.addLast(mWordObj);
                    synchronized (newWordAddedSync) {
                        if (VERBOSE) Log.d(TAG, "Notifying that next word was added.");
                        newWordAddedSync.notify();
                        spritzerWaitingForWord = false;
                    }
                }
                addWordsExecutorQueueSize --;
            }});
        /*
        addWordsThread = new Thread(new Runnable() {
            public void run() {
                addWordsThreadRunning = true;
                String mInput = input;
                while (mInput != null && !killAddWordsThread){
                    if (VERBOSE) Log.d(TAG, "Adding remaining text: " + mInput);
                    WordObj mWordObj = mWordStrategy.parseWord(mInput);
                    mInput = mWordObj.remainingWords;
                    mWordQueue.addLast(mWordObj);
                    synchronized (newWordAddedSync) {
                        if (VERBOSE) Log.d(TAG, "Spritzer was waiting for next word. Notifying");
                        newWordAddedSync.notify();
                        spritzerWaitingForWord = false;
                    }
                }
                if (VERBOSE) Log.d(TAG, "addWordsThread Complete, notifying.");
                addWordsThreadRunning = false;
                synchronized (addWordsThreadSync) {
                    addWordsThreadSync.notify();
                }
            }
        });
        addWordsThread.start();
        */
        setMaxProgress();
    }

    private boolean addWordsExecutorRunning(){
        if (VERBOSE) Log.d(TAG, "addWordsExecutorQueueSize; " + addWordsExecutorQueueSize);
        return addWordsExecutorQueueSize > 0;
    }

    private void emptyWordQueue() throws InterruptedException {
        if (VERBOSE) Log.d(TAG, "Emptying Word Queue");
        while (!addWordsExecutor.isShutdown()){
            addWordsExecutor.shutdownNow();
            addWordsExecutor.awaitTermination(100, TimeUnit.MILLISECONDS);
        }
        addWordsExecutor = Executors.newSingleThreadExecutor();
        if (VERBOSE) Log.d(TAG, "Finished waititing for addWordsExecutor to shutdown.");
        mCurWordIdx = 0;
        if (!mWordQueue.isEmpty()) mWordQueue.clear();
        updateProgress();
        if (VERBOSE) Log.d(TAG, "Finished emptyWordQueue.");
    }

    private void setMaxProgress() {
        if (VERBOSE) Log.d(TAG, "Setting max progress for progress bar to: " + mWordQueue.size());
        if (mWordQueue != null && mProgressBar != null) {
            mProgressBar.setMax(mWordQueue.size());
        }
    }

    public int getMSRemainingInQueue() {
        if (mWordQueue.size() == 0) {
            return 0;
        }
        int sum = 0;
        for (WordObj mWordObj : mWordQueue) {
            sum += Math.round(mWordObj.delayMultiplier * mBaseDelay);
        }
        return sum;
    }

    public int getWpm() {
        if (VERBOSE) Log.d(TAG, "Returning wpm:  " + 60000 / mBaseDelay);
        return 60000 / mBaseDelay;
    }

    // Set the WPM. if recompute is true, the delay for all words currently in the queue will be recalculated. If not, only future words will be added with the new delay.
    public void setWpm(int wpm) {
        if (VERBOSE) Log.d(TAG, "Setting Delay to " + wpm + " wpm.");
        mBaseDelay = 60000 / wpm;
        if (VERBOSE) Log.d(TAG, "mBaseDelay is now: " + mBaseDelay);
    }

    /**
     * Swap the target TextView. Call this if your
     * host Activity is Destroyed and Re-Created.
     * Effective immediately.
     *
     * @param target
     */
    public void swapTextView(TextView target) {
        if (VERBOSE) Log.d(TAG, "Swapping Text View.");
        mTarget = target;
        if (!mPlaying) {
            printLastWord();
        }

    }

    /**
     * Start displaying the String input
     * fed to {@link #setText(String)}
     */
    public void start() {
        if (VERBOSE) Log.d(TAG, "Starting spritzer.");
        mPlayingRequested = true;
        processNextWord();
    }

    private void updateProgress() {
        if (VERBOSE) Log.d(TAG, "Updating Progress.");
        if (mProgressBar != null) {
            mProgressBar.setProgress(mCurWordIdx);
        }
    }


    private String mLastWord;              // Holds the string of the last word played. Useful for pausing ect...
    private boolean spritzerWaitingForWord; // returns true if the spritzer is stuck waiting for the next word to be added to the queue.


    private ScheduledExecutorService processWordExecutor = Executors.newScheduledThreadPool(1);

    int previousWordQueueLength = 0;
    protected void processNextWord() {   // Reads the current word queue one word at a time, pausing for delay
        if (VERBOSE) Log.d(TAG, "Starting processNextWord.");
        while (mWordQueue.isEmpty() && addWordsExecutorRunning()) {
            if (VERBOSE) Log.d(TAG, "mWordQueue.isEmpty() and addWordsExecutor is running, must wait for next word.");
            long threadDelayTime = 0;
            if (VERBOSE) threadDelayTime = System.nanoTime();
            synchronized (newWordAddedSync) {
                try {
                    newWordAddedSync.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (VERBOSE) Log.d(TAG, "Delayed waiting for next word " + TimeUnit.MILLISECONDS.convert(System.nanoTime() - threadDelayTime, TimeUnit.NANOSECONDS) + "ms");
        }
        if (!mWordQueue.isEmpty()) {
            if (VERBOSE) Log.d(TAG, "mWordQueue is not empty, and mPlayingRequested. Displaying next word.");
            final WordObj mWordObj = mWordQueue.remove();
            if (VERBOSE) Log.d(TAG, "Sending " + mWordObj.parsedWord + " to text view.");
            mSpritzHandler.sendMessage(mSpritzHandler.obtainMessage(MSG_PRINT_WORD, mWordObj.parsedWord));
            if (mWordObj.callback != 0){
                mTarget.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallBackListener != null) {
                            mCallBackListener.onCallBackListener(mWordObj.callback);
                        }
                    }
                });
            }

            if ((!mWordQueue.isEmpty() || addWordsExecutorRunning()) && mPlayingRequested) {
                mPlaying = true;
                if (VERBOSE) Log.d(TAG, "mWordQueue still not empty, and schedule the next run.");
                // processWordExecutor = Executors.newScheduledThreadPool(1);
                processWordExecutor.schedule(new Runnable() {
                    public void run() {
                        processNextWord();
                    }}, (long) Math.round(mWordObj.delayMultiplier * mBaseDelay), TimeUnit.MILLISECONDS);
                if (VERBOSE) Log.i(TAG, "Scheduled next run in " + (long) Math.round(mWordObj.delayMultiplier * mBaseDelay) + " MS");
            }
            if ((mWordQueue.isEmpty() && !addWordsExecutorRunning()) || !mPlayingRequested) {
                if (VERBOSE) Log.i(TAG, "Stopping spritzThread");
                mPlaying = false;
                mPlayingRequested = false;
//                mSpritzThreadStarted = false;
            }
            if (mWordQueue.size() < previousWordQueueLength) { // check that mWordQueue is shrinking before triggering listener.
                if (mWordQueue.size() <= wordsRemainingToTriggerListener) {
                    mTarget.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mRemainingWordsListener != null) {
                                mRemainingWordsListener.onWordsRemaining();
                            }
                        }
                    });
                }
            }
            mCurWordIdx += 1;
            mLastWord = mWordObj.parsedWord;
            previousWordQueueLength = mWordQueue.size();
        }
        updateProgress();
    }

    private void printLastWord() {
        printWord(mLastWord);
    }

    protected TextView mTarget;
    protected Handler mSpritzHandler;
    public Spritzer(TextView target) {      // Initialize the Spritzer
        if (VERBOSE) Log.d(TAG, "Initializing the Spritzer");
        init();
        mTarget = target;
        mSpritzHandler = new SpritzHandler(this);
    }

    private void printWord(String word) {
        if (VERBOSE) Log.d(TAG, "printing word: " + word);
        int startSpan = 0;
        int endSpan = 0;
        word = word.trim();
        if (VERBOSE) Log.i(TAG + word.length(), word);
        if (word.length() == 1) {
            StringBuilder builder = new StringBuilder();
            for (int x = 0; x < CHARS_LEFT_OF_PIVOT; x++) {
                builder.append(" ");
            }
            builder.append(word);
            word = builder.toString();
            startSpan = CHARS_LEFT_OF_PIVOT;
            endSpan = startSpan + 1;
        } else if (word.length() <= CHARS_LEFT_OF_PIVOT * 2) {
            StringBuilder builder = new StringBuilder();
            int halfPoint = word.length() / 2;
            int beginPad = CHARS_LEFT_OF_PIVOT - halfPoint;
            for (int x = 0; x <= beginPad; x++) {
                builder.append(" ");
            }
            builder.append(word);
            word = builder.toString();
            startSpan = halfPoint + beginPad;
            endSpan = startSpan + 1;
            if (VERBOSE) Log.i(TAG + word.length(), "pivot: " + word.substring(startSpan, endSpan));
        } else {
            startSpan = CHARS_LEFT_OF_PIVOT;
            endSpan = startSpan + 1;
        }

        Spannable spanRange = new SpannableString(word);
        TextAppearanceSpan tas = new TextAppearanceSpan(mTarget.getContext(), R.style.PivotLetter);
        spanRange.setSpan(tas, startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mTarget.setText(spanRange);
    }

    public void pause() {
        if (VERBOSE) Log.d(TAG, "Pausing.");
        mPlayingRequested = false;
    }

    public boolean isPlaying() {
        return mPlaying;
    }

    // Setup completion listener which is called when all spritzing is done:
    private RemainingWordsListener mRemainingWordsListener;  // Set the remainingWords listener.
    public interface RemainingWordsListener {  // Interface to be overwritten to perform actions when spritzing is complete
        public void onWordsRemaining();
    }
    public void setRemainingWordsListener(RemainingWordsListener remainingWordsListener) {    // Set the complete listener for completion
        mRemainingWordsListener = remainingWordsListener;
    }
    public int wordsRemainingToTriggerListener = 0;
    public void setWordsRemainingToTriggerListener(int remainingWords){
        wordsRemainingToTriggerListener = remainingWords;
    }

    // callback listener for callback metadata in word object.
    private CallBackListener mCallBackListener;  // Set the remainingWords listener.
    public interface CallBackListener {  // Interface to be overwritten to perform actions when spritzing is complete
        public void onCallBackListener(int callback);
    }
    public void setCallBackListener(CallBackListener callBackListener) {    // Set the complete listener for completion
        mCallBackListener = callBackListener;
    }

    // Background timer thread
    /*
    protected Object mPlayingSync = new Object();
    private void startTimerThread() {
        if (VERBOSE) Log.d(TAG, "Starting timer thread.");
        synchronized (mPlayingSync) {
            if (!mSpritzThreadStarted) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (VERBOSE) Log.i(TAG, "Starting spritzThread with queue length " + mWordQueue.size());

                        mPlaying = true;
                        mSpritzThreadStarted = true;

                        while (mPlayingRequested) {
                            try {
                                if (mWordQueue.isEmpty() || addWordsThreadRunning) {
                                    spritzerWaitingForWord = true;
                                    long threadDelayTime = 0;
                                    if (VERBOSE) threadDelayTime = System.nanoTime();
                                    while (spritzerWaitingForWord) {
                                        synchronized (newWordAddedSync) {
                                            newWordAddedSync.wait();
                                        }
                                    }
                                    if (VERBOSE) Log.d(TAG, "Delayed waiting for next word " + TimeUnit.MILLISECONDS.convert(System.nanoTime() - threadDelayTime, TimeUnit.NANOSECONDS) + "ms");
                                }

                                processNextWord();

                                if (mWordQueue.isEmpty() && !addWordsThreadRunning) {

                                    if (VERBOSE)
                                        Log.i(TAG, "Queue is empty after processNextWord. Pausing");

                                    mTarget.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (mRemainingWordsListener != null) {
                                                mRemainingWordsListener.onWordsRemaining();
                                            }
                                        }
                                    });

                                    mPlayingRequested = false;
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        if (VERBOSE) Log.i(TAG, "Stopping spritzThread");

                        mPlaying = false;
                        mSpritzThreadStarted = false;

                    }
                }).start();
            }
        }
    }
    */

    public ArrayDeque<WordObj> getWordQueue() {
        return mWordQueue;
    }

    private ProgressBar mProgressBar;      // Progress bar for viewing progress through spritzing.
    public void attachProgressBar(ProgressBar bar) {
        if (VERBOSE) Log.d(TAG, "Attaching progress bar.");
        if (bar != null) {
            mProgressBar = bar;
        }
    }

    /**
     * A Handler intended for creation on the Main thread.
     * Messages are intended to be passed from a background
     * timing thread. This Handler communicates timing
     * thread events to the Main thread for UI update.
     */
    protected static class SpritzHandler extends Handler {
        private WeakReference<Spritzer> mWeakSpritzer;

        public SpritzHandler(Spritzer muxer) {
            mWeakSpritzer = new WeakReference<Spritzer>(muxer);
        }

        @Override
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            Spritzer spritzer = mWeakSpritzer.get();
            if (spritzer == null) {
                return;
            }

            switch (what) {
                case MSG_PRINT_WORD:
                    spritzer.printWord((String) obj);
                    break;
                default:
                    throw new RuntimeException("Unexpected msg what=" + what);
            }
        }

    }


}