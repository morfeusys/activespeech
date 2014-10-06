package com.example;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

import static android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH;
import static android.speech.RecognizerIntent.EXTRA_CALLING_PACKAGE;
import static android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL;
import static android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM;

/**
 * Created by morfeusys on 02.03.14.
 */
public class SpeechRecognitionService extends Service {

    private static final String TAG = "RecognitionService";

    private final Handler mHandler = new Handler();

    private RecognitionBinder mRecognitionBinder;

    @Override
    public void onCreate() {
        mRecognitionBinder = new RecognitionBinder();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mRecognitionBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mRecognitionBinder.onUnbind();
        return false;
    }

    public class RecognitionBinder extends Binder {
        private Listener mListener;
        private boolean mStarted;
        private SpeechRecognizer mSpeechRecognizer;

        public void onUnbind() {
            destroyRecognizer();
        }

        public void setListener(Listener listener) {
            mListener = listener;
        }

        private synchronized void createRecognizer() {
            if(mSpeechRecognizer == null) {
                Log.d(TAG, "Creating speech recognizer");
                mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(SpeechRecognitionService.this);
                mSpeechRecognizer.setRecognitionListener(new Callback());
            }
        }

        private synchronized void destroyRecognizer() {
            if(mSpeechRecognizer != null) {
                Log.d(TAG, "Destroying speech recognizer");
                mSpeechRecognizer.setRecognitionListener(null);
                mSpeechRecognizer.destroy();
                mSpeechRecognizer = null;
            }
        }

        private void fireBeginningOfSpeech() {
            if (mListener != null) {
                Log.d(TAG, "onBeginningOfSpeech");
                mListener.onBeginningOfSpeech();
            }
        }

        private void fireEndOfSpeech() {
            if (mListener != null) {
                Log.d(TAG, "onEndOfSpeech");
                mListener.onEndOfSpeech();
            }
        }

        private void fireReadyForSpeech() {
            if (mListener != null) {
                Log.d(TAG, "onReadyForSpeech");
                mListener.onReadyForSpeech();
            }
        }

        private void fireResult(String text) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    destroyRecognizer();
                }
            });
            if (mListener != null) {
                if(text == null) {
                    Log.d(TAG, "onResult(null)");
                } else {
                    Log.d(TAG, "onResult " + text);
                }
                mListener.onResult(text);
            }
        }

        public void start() {
            Log.d(TAG, "start");
            final Intent intent = new Intent(ACTION_RECOGNIZE_SPEECH)
                    .putExtra(EXTRA_CALLING_PACKAGE, getPackageName())
                    .putExtra(EXTRA_LANGUAGE_MODEL, LANGUAGE_MODEL_FREE_FORM);
            mStarted = true;
            startListening(intent);
        }

        public void cancel() {
            Log.d(TAG, "cancel");
            mStarted = false;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mSpeechRecognizer != null) {
                        mSpeechRecognizer.cancel();
                        destroyRecognizer();
                    }
                }
            });
        }

        private void startListening(final Intent intent) {
            Log.d(TAG, "startListening");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    createRecognizer();
                    mSpeechRecognizer.startListening(intent);
                }
            });
        }

        public boolean isStarted() {
            return mStarted;
        }

        private class Callback implements android.speech.RecognitionListener {

            @Override
            public void onBeginningOfSpeech() {
                fireBeginningOfSpeech();
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                fireEndOfSpeech();
            }

            @Override
            public void onError(int code) {
                mStarted = false;
                fireResult(null);
            }

            @Override
            public void onEvent(int arg0, Bundle arg1) {
            }

            @Override
            public void onPartialResults(Bundle arg0) {
            }

            @Override
            public void onReadyForSpeech(Bundle arg0) {
                fireReadyForSpeech();
            }

            @Override
            public void onResults(Bundle bundle) {
                mStarted = false;
                final ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                final String text;
                if (matches == null || matches.isEmpty()) {
                    text = null;
                } else {
                    text = matches.get(0);
                }
                fireResult(text);
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }
        }
    }

    public static interface Listener {
        void onReadyForSpeech();
        void onBeginningOfSpeech();
        void onEndOfSpeech();
        void onResult(String text);
    }
}
