package com.example;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;


public class MainActivity extends Activity {

    private static final String KWS_SEARCH = "kws";
    private static final String KWS_PHRASE = "умный дом";

    private final Handler mHandler = new Handler();

    private TextView mStatusView;
    private SpeechRecognizer mVoiceActivator;
    private SpeechRecognitionService.RecognitionBinder mSpeechRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mStatusView = (TextView) findViewById(R.id.status);
        bindService(new Intent(this, SpeechRecognitionService.class), mRecognizerConnection, Context.BIND_AUTO_CREATE);
        prepareVoiceActivator();
    }

    @Override
    protected void onDestroy() {
        cancelKeyphraseRecognition();
        cancelSpeechRecognition();
        unbindService(mRecognizerConnection);
        super.onDestroy();
    }

    private void prepareVoiceActivator() {
        mStatusView.setText("Preparing");
        final DataFiles df = new DataFiles(getPackageName(), "ru");
        final Dict dict = new Dict();
        dict.add("умный", "uu m n ay j").add("дом", "d oo m");
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    File dictFile = new File(df.getDict());
                    copyAssets(new File(df.getHmm()));
                    saveFile(dictFile, dict.toString());
                    mVoiceActivator = SpeechRecognizerSetup.defaultSetup()
                            .setAcousticModel(new File(df.getHmm()))
                            .setDictionary(dictFile)
                            .setBoolean("-remove_noise", false)
                            .setKeywordThreshold(1e-5f)
                            .getRecognizer();
                    mVoiceActivator.addKeyphraseSearch(KWS_SEARCH, KWS_PHRASE);
                    mVoiceActivator.addListener(mVoiceActivatorListener);
                    return null;
                } catch (IOException e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Exception e) {
                if (e != null) {
                    mStatusView.setText("Error");
                } else {
                    startKeyphraseRecognition();
                }
            }
        }.execute();
    }

    private synchronized void startKeyphraseRecognition() {
        mStatusView.setText("Ready");
        if (mVoiceActivator != null) mVoiceActivator.startListening(KWS_SEARCH);
    }

    private synchronized void cancelKeyphraseRecognition() {
        if (mVoiceActivator != null) mVoiceActivator.cancel();
    }

    private synchronized void startSpeechRecognition() {
        cancelKeyphraseRecognition();
        if (mSpeechRecognizer != null) mSpeechRecognizer.start();
    }

    private synchronized void cancelSpeechRecognition() {
        if (mSpeechRecognizer != null) mSpeechRecognizer.cancel();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void copyAssets(File baseDir) throws IOException {
        String[] files = getAssets().list("hmm/ru");
        for (String fromFile : files) {
            File toFile = new File(baseDir.getAbsolutePath() + "/" + fromFile);
            InputStream in = getAssets().open("hmm/ru/" + fromFile);
            FileUtils.copyInputStreamToFile(in, toFile);
        }
    }

    private void saveFile(File f, String content) throws IOException {
        File dir = f.getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create directory: " + dir);
        }
        FileUtils.writeStringToFile(f, content, "UTF8");
    }

    private final ServiceConnection mRecognizerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mSpeechRecognizer = (SpeechRecognitionService.RecognitionBinder) service;
            mSpeechRecognizer.setListener(mSpeechRecognitionListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSpeechRecognizer = null;
        }
    };

    private final RecognitionListener mVoiceActivatorListener = new RecognitionListener() {
        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onPartialResult(Hypothesis hypothesis) {
            if (hypothesis != null) {
                startSpeechRecognition();
            }
        }

        @Override
        public void onResult(Hypothesis hypothesis) {
        }
    };

    private final SpeechRecognitionService.Listener mSpeechRecognitionListener = new SpeechRecognitionService.Listener() {
        @Override
        public void onReadyForSpeech() {
            mStatusView.setText("Say");
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onResult(String text) {
            Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startKeyphraseRecognition();
                }
            }, 300);
        }
    };
}
