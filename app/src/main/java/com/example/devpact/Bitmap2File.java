package com.example.devpact;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;

/**
 * Created by Debosmit on 2/12/16.
 */
public class Bitmap2File extends AsyncTask<Void, Void, File> {

    private AsyncTaskResponse thisResponse = null;
    private final WeakReference<Activity> mActivity;
    private final Bitmap scaledBitmap;
    private final File compressedFile;

    public Bitmap2File(Bitmap scaledBitmap, File outFile, Activity activity, AsyncTaskResponse fileResponse) {
        this.compressedFile = outFile;
        this.scaledBitmap = scaledBitmap;
        this.mActivity = new WeakReference<Activity>(activity);
        this.thisResponse = fileResponse;
    }

    @Override
    protected File doInBackground(Void... voids) {
        // debug in background task
        android.os.Debug.waitForDebugger();

        try {
            FileOutputStream outputStream = new FileOutputStream(compressedFile);

            // if activity isn't around anymore
            if(mActivity == null)
                return compressedFile;

            int QUALITY = mActivity.get().getResources().getInteger(R.integer.COMPRESS_QUALITY);

            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, outputStream);
            outputStream.close();
        } catch(Exception e) {
            return null;
        }
        return compressedFile;
    }

    @Override
    protected void onPostExecute(File compressedFile) {
        thisResponse.onAsyncTaskComplete(compressedFile);
    }
}
