package com.example.devpact;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Created by Debosmit on 2/11/16.
 */
public class BitmapCompress extends AsyncTask<Void, Void, File> {



    private final String imgFilepath;
    private final WeakReference<Activity> mActivity;

    public BitmapCompress(String OrigImgFilepath, Activity activity) {
        this.imgFilepath = OrigImgFilepath;
        this.mActivity = new WeakReference<Activity>(activity);
    }

    @Override
    protected File doInBackground(Void... voids) {
        // debug in background task
        android.os.Debug.waitForDebugger();

        // check if activity is still available
        // no point doing any work otherwise
        if(mActivity == null) {
            return new File(imgFilepath);
        }

        // max file size
        // stored in constants.xml
        int MAX_SIZE = mActivity.get().getResources().getInteger(R.integer.MAX_IMAGE_SIZE_BYTES);

        // if file does not exists, there is nothing to do
        // if file is below limit, nothing to do
        if(new File(imgFilepath).length() <= MAX_SIZE)
            return new File(imgFilepath);

        // get bitmap from filepath
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;  // setting to true, since don't need bitmap
                                            // returned at this point => that would lead
                                            // to memory wastage
        Bitmap bmp = BitmapFactory.decodeFile(imgFilepath, options);

        // compress bitmap if required


        // pipe compressed bitmap to new file output stream

        // return compressed file, or original file if
        // compressed file is larger than original file
        return null;
    }

    @Override
    protected void onPostExecute(File CompressedFile) {

    }
}