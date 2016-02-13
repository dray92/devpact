package com.example.devpact;

import android.graphics.Bitmap;

import java.io.File;

/**
 * Created by Debosmit on 2/12/16.
 */
public interface AsyncTaskResponse {
    void onAsyncTaskComplete(Bitmap bitmap);
    void onAsyncTaskComplete(File compressedImg);
}
