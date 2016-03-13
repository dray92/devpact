package com.example.devpact;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsSharedAccessSignature;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class DisplayImageActivity extends Activity {

    private File mPhotoFile;

    private String mLastLatitude;
    private String mLastLongitude;
    private String mPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_image);

        Intent intent = getIntent();
        mPhotoFile = (File)intent.getExtras().get("image");
        mLastLatitude = (String)intent.getExtras().get("lat");
        mLastLongitude = (String)intent.getExtras().get("long");
        mPhoneNumber = (String)intent.getExtras().get("phone");

        ImageView mImageView = (ImageView) findViewById(R.id.imageView);

        int aspectRatio = getAspectRatio();

        mImageView.setImageBitmap(
                decodeSampledBitmap(100,100));

        Button mUploadButton = (Button) findViewById(R.id.upload);
        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadPhoto(view);
            }
        });
    }

    public int getAspectRatio() {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mPhotoFile.getAbsolutePath(), options);

        return options.outHeight/options.outWidth;
    }

    public Bitmap decodeSampledBitmap(int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mPhotoFile.getAbsolutePath(), options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(mPhotoFile.getAbsolutePath());
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
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

    /**
     * Add a new item
     *
     * @param view
     *            The view that originated the call
     */
    public void uploadPhoto(View view) {

        Log.d("Upload Photo", "Entered");
        Toast.makeText(DisplayImageActivity.this, "Starting upload process", Toast.LENGTH_LONG);

        // check if file is not readable for some reason
        if(new File(mPhotoFile.getPath()).length() == 0)
            Toast.makeText(DisplayImageActivity.this, "Upload failed - File Access Error", Toast.LENGTH_SHORT).show();

        try {
            File compressedFile = getTempFile();
            BitmapCompress bitmapCompress = new BitmapCompress(mPhotoFile.getPath(), compressedFile, this);
            bitmapCompress.execute();
        } catch(Exception e) {
            startUploadTask();
        }

    }

    private File getTempFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir =
//                getApplicationContext().getExternalFilesDir(null);
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
//                getApplicationContext().getFilesDir();

        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    /**
     * Helper method to start an upload task
     */
    private void startUploadTask() {
        // Create a new item
        final ToDoItem item = new ToDoItem();

        // if user doesn't add description
        EditText textBox = (EditText)findViewById(R.id.description);
        String text = textBox.getText().toString();
        if(text.length() == 0) {
            text = mPhoneNumber + "(" + mLastLatitude + "," + mLastLongitude + ")";
        }

        item.setText(text);
        item.setComplete(false);
        item.setContainerName("todoitemimages");
        item.setPhoneNumber(mPhoneNumber);
        item.setLatitude(mLastLatitude);
        item.setLongitude(mLastLongitude);

        // Use a unigue GUID to avoid collisions.
        UUID uuid = UUID.randomUUID();
        String uuidInString = uuid.toString();
        item.setResourceName(uuidInString);

        // Send the item to be inserted. When blob properties are set this
        // generates an SAS in the response.
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // debug in background task
                android.os.Debug.waitForDebugger();

                try {
                    final ToDoItem entity = ToDoActivity.addItemInTable(item);

                    // If we have a returned SAS, then upload the blob.
                    if (entity.getSasQueryString() != null) {

                        // Get the URI generated that contains the SAS
                        // and extract the storage credentials.
                        StorageCredentials cred =
                                new StorageCredentialsSharedAccessSignature(entity.getSasQueryString());
                        URI imageUri = new URI(entity.getImageUri());

                        // Upload the new image as a BLOB from a stream.
                        CloudBlockBlob blobFromSASCredential =
                                new CloudBlockBlob(imageUri, cred);

                        blobFromSASCredential.uploadFromFile(mPhotoFile.getPath());
                        Log.d("Azure", "Upload started");
                        Toast.makeText(DisplayImageActivity.this, "Azure upload started", Toast.LENGTH_LONG);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!entity.isComplete()) {
                                ToDoActivity.mAdapter.add(entity);
                            }
                        }
                    });
                } catch (final Exception e) {
                    Toast.makeText(DisplayImageActivity.this, e.toString(), Toast.LENGTH_LONG);
                }
                return null;
            }
        };

        ToDoActivity.runAsyncTask(task);
        onBackPressed();
    }


    public class BitmapCompress extends AsyncTask<Void, Void, File> {

        private final String imgFilepath;
        private final WeakReference<Activity> mActivity;
        private final File newFile;

        public BitmapCompress(String OrigImgFilepath, File newFile, Activity activity) {
            this.imgFilepath = OrigImgFilepath;
            this.mActivity = new WeakReference<Activity>(activity);
            this.newFile = newFile;
        }

        @Override
        protected File doInBackground(Void... voids) {
            Log.d("Compressing Photo","Entered task");
            Toast.makeText(DisplayImageActivity.this, "Compressing photo", Toast.LENGTH_LONG);
            // debug in background task
            android.os.Debug.waitForDebugger();

            // check if activity is still available
            // no point doing any work otherwise
            if(mActivity == null)
                return new File(imgFilepath);

            // max file size
            // stored in constants.xml
            Resources mActivityResources = mActivity.get().getResources();
            int MAX_SIZE = mActivityResources.getInteger(R.integer.MAX_IMAGE_SIZE_BYTES);

            // if file does not exists, there is nothing to do
            // if file is below limit, nothing to do
            if(new File(imgFilepath).length() <= MAX_SIZE)
                return new File(imgFilepath);

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

            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(newFile);
                int QUALITY = mActivityResources.getInteger(R.integer.COMPRESS_QUALITY);

                compressedBmp.compress(Bitmap.CompressFormat.JPEG, QUALITY, outputStream);
                outputStream.close();
                return newFile;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return new File(imgFilepath);
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
        protected void onPostExecute(File CompressedBmp) {
            Log.d("Compressing Photo","Compressed photo");
            Toast.makeText(DisplayImageActivity.this, "Compressed photo", Toast.LENGTH_LONG);
            mPhotoFile = CompressedBmp;
            startUploadTask();
        }
    }
}
