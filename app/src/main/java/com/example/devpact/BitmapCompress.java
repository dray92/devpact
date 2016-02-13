package com.example.devpact;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Created by Debosmit on 2/11/16.
 */
public class BitmapCompress extends AsyncTask<Void, Void, Bitmap> {

    private AsyncTaskResponse thisResponse = null;
    private final String imgFilepath;
    private final WeakReference<Activity> mActivity;

    public BitmapCompress(String OrigImgFilepath, Activity activity) {
        this.imgFilepath = OrigImgFilepath;
        this.mActivity = new WeakReference<Activity>(activity);
    }

    @Override
    protected Bitmap doInBackground(Void... voids) {
        // debug in background task
        android.os.Debug.waitForDebugger();

        // check if activity is still available
        // no point doing any work otherwise
        if(mActivity == null) {
            return BitmapFactory.decodeFile(imgFilepath);
        }

        // max file size
        // stored in constants.xml
        Resources mActivityResources = mActivity.get().getResources();
        int MAX_SIZE = mActivityResources.getInteger(R.integer.MAX_IMAGE_SIZE_BYTES);

        // if file does not exists, there is nothing to do
        // if file is below limit, nothing to do
        if(new File(imgFilepath).length() <= MAX_SIZE)
            return BitmapFactory.decodeFile(imgFilepath);

        // get bitmap from filepath
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;  // setting to true, since don't need bitmap
                                            // returned at this point => that would lead
                                            // to memory wastage
        BitmapFactory.decodeFile(imgFilepath, options);

        // get target dimensions
        int targetHeight = mActivityResources.getInteger(R.integer.TARGET_IMAGE_HEIGHT);
        int targetWidth = mActivityResources.getInteger(R.integer.TARGET_IMAGE_WIDTH);

        // compress bitmap
        /* greater dimension needs to be scaled down to 512
         * other dimension will be scaled by same value
         * if height > width => portrait mode, then continue as normal ....
         * else, height < width
         *      => landscape mode
         *      => swap targetHeight, targetWidth
         */
        if(options.outHeight < options.outWidth) {
            // swap targetHeight and targetWidth
            targetHeight ^= targetWidth;
            targetWidth ^= targetHeight;
            targetHeight ^= targetWidth;
        }
        int sampleSize = calculateInSampleSize(options, targetWidth, targetHeight);

        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        Bitmap compressedBmp = BitmapFactory.decodeFile(imgFilepath, options);

        // return compressed file, or original file if
        // compressed file is larger than original file
        return compressedBmp;
    }

    // required
    private int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {

        // check if activity is still available
        // no point doing any work otherwise
        if(mActivity == null)
            return 1;

        int minDim = mActivity.get().getResources().getInteger(R.integer.MINIMUM_DIMENSION);
        if(reqHeight < minDim || reqWidth < minDim)
            return 1;

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    @Override
    protected void onPostExecute(Bitmap CompressedBmp) {
        this.thisResponse.onAsyncTaskComplete(CompressedBmp);
    }
}