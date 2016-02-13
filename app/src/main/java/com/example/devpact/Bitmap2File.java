package com.example.devpact;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Created by Debosmit on 2/12/16.
 */
public class Bitmap2File extends AsyncTask<Void, Void, File> {

    private AsyncTaskResponse thisResponse = null;
    private final WeakReference<Activity> mActivity;
    private final Bitmap bitmap;
    private final File compressedFile;

    public Bitmap2File(Bitmap bmp, File outFile, Activity activity) {
        this.compressedFile = outFile;
        this.bitmap = bmp;
        this.mActivity = new WeakReference<Activity>(activity);
    }

    @Override
    protected File doInBackground(Void... voids) {
        return null;
    }

    @Override
    protected void onPostExecute(File compressedFile) {

    }
}
