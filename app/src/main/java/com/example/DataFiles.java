package com.example;

import android.os.Environment;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class DataFiles {

    private static final int VERSION = 1;

	private final int mSampleRateInHz;

	private final File mFileHmm;
	private final File mFileJsgf;
	private final File mFileDict;
	private final File mFileLog;
	private final File mDirRawLog;

    public DataFiles(String packageName, String lang) {
        this(packageName, lang, 16000);
    }

    public DataFiles(String packageName, String lang, int sampleRate) {
        String baseDirAsString = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + packageName + "/" + lang + "/" + VERSION;

        mFileHmm = new File(baseDirAsString +"/hmm/" + sampleRate);
        mFileJsgf = new File(baseDirAsString + "/lm/" + "lm.jsgf");
        mFileDict = new File(baseDirAsString + "/lm/" + "lm.dic");
        mFileLog = new File(baseDirAsString + "/pocketsphinx.log");
        mDirRawLog = new File(baseDirAsString + "/raw/");
        mSampleRateInHz = sampleRate;
    }

	public boolean deleteDict() {
		return mFileDict.delete();
	}

	public boolean deleteLogfile() {
		return mFileLog.delete();
	}

	public boolean deleteJsgf() {
		return mFileJsgf.delete();
	}

	public boolean deleteRawLogDir() {
		try {
			FileUtils.cleanDirectory(mDirRawLog);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public boolean createRawLogDir() {
		if (! mDirRawLog.exists()) {
			try {
				FileUtils.forceMkdir(mDirRawLog);
			} catch (IOException e) {
				return false;
			}
		}
		return true;
	}

	public String getLogfile() {
		return mFileLog.getAbsolutePath();
	}

	public String getRawLogDir() {
		return mDirRawLog.getAbsolutePath();
	}

	public String getHmm() {
		return mFileHmm.getAbsolutePath();
	}

	public String getDict() {
		return mFileDict.getAbsolutePath();
	}

	public String getJsgf() {
		return mFileJsgf.getAbsolutePath();
	}

	public int getSampleRateInHz() {
		return mSampleRateInHz;
	}

}