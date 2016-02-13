package com.example.devpact;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsSharedAccessSignature;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceException;
import com.microsoft.windowsazure.mobileservices.UserAuthenticationCallback;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.http.NextServiceFilterCallback;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilter;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterRequest;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.microsoft.windowsazure.mobileservices.table.query.QueryOperations.val;


public class ToDoActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, AsyncTaskResponse {

    /**
     * Mobile Service Client reference
     */
    private MobileServiceClient mClient;


    /**
     * Mobile Service Table used to access data
     */
    private MobileServiceTable<ToDoItem> mToDoTable;


    /**
     * Adapter to sync the items list with the view
     */
    private ToDoItemAdapter mAdapter;


    /**
     * EditText containing the "New To Do" text
     */
    private EditText mTextNewToDo;


    /**
     * Progress spinner to use for table operations
     */
    private ProgressBar mProgressBar;


    /**
     * variables for photograph
     */
    static final int REQUEST_TAKE_PHOTO = 1;
    public Uri mPhotoFileUri = null;
    public File mPhotoFile = null;
    private static final String PHOTO_SAVED_BUNDLE = "myPhotoFile";
    private boolean mPhotoFileExists = false;


    /**
     * cache authentication tokens
     */
    public static final String SHAREDPREFFILE = "temp";
    public static final String USERIDPREF = "uid";
    public static final String TOKENPREF = "tkn";


    /**
     * refresh authorization tokens
     */
    public boolean bAuthenticating = false;
    public final Object mAuthenticationLock = new Object();


    /**
     * constants needed to prompt user to
     * login if the previous token expired
     */
    private final String AUTH_DIALOG_TITLE = "Choose a Login Method";
    private final String[] AUTH_SERVERS = {"Google Account (Gmail)", "Facebook", "Microsoft Outlook"};


    /**
     * Client device phone number
     */
    private String mPhoneNumber = "";


    /**
     * Picture location data
     */
    private Location mLastLocation;
    private String mLastLatitude = "";
    private String mLastLongitude = "";
    private static final String DEFAULT_LOCATION_NOT_FOUND = "Location is NULL";
    private LocationRequest mLocationRequest = null;
    private static final String LOCATION_SAVED_BUNDLE = "myLastLocationObject";


    /**
     * Google API client for location
     */
    private GoogleApiClient mGoogleApiClient;


    /**
     * Checks if authentication is required
     */
    private static final boolean OAUTH_REQUIRED = true;


    /**
     *
     * Keep track of asking user for permission at runtime
     */
    private static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 0xaabbcc;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0xabcabc;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 0xcbaabc;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_EXTERNAL_STORAGE = 0xcbacba;
    private static final int LOCATION_REQUEST_CHECK_SETTINGS = 0xabccba;
    private int PERMISSION_ACCESS_FINE_LOCATION = 0;
    private int PERMISSION_ACCESS_COARSE_LOCATION = 0;
    private int PERMISSION_ACCESS_EXTERNAL_STORAGE = 0;
    private int PERMISSION_READ_PHONE_STATE = 0;


    /**
     * to restore states, app must run at least once,
     * to set the appropriate variables
     */
    private boolean hasRunOnce = false;

    /**
     * Initializes the activity
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_to_do);
        mProgressBar = (ProgressBar) findViewById(R.id.loadingProgressBar);

        // Initialize the progress bar
        mProgressBar.setVisibility(ProgressBar.GONE);

        if(mClient == null) {
            try {
                // Create the Mobile Service Client instance, using the provided
                // Mobile Service URL and key
                mClient = new MobileServiceClient(
                        "https://devpact.azure-mobile.net/",
                        "TxUfvhAhXNpJPtrsFzWLQyuWPlwbgW11", this)
                        .withFilter(new ProgressFilter())
                        .withFilter(new RefreshTokenCacheFilter());

                // Authenticate passing false to load the current token cache if available.
                authenticate(false);

            } catch (MalformedURLException e) {
                createAndShowDialog(new Exception("Error creating the Mobile Service. " +
                        "Verify the URL"), "Error");
            }
        }

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        updateDataFromBundle(savedInstanceState);
    }


    private void retrievePhoneNumber() {

        // External storage write permission
        PERMISSION_READ_PHONE_STATE = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE);

        /**
         * Check if permissions were set
         * If so, return true
         */
        if(PERMISSION_READ_PHONE_STATE == PackageManager.PERMISSION_GRANTED) {
            // get user's phone number
            try {
                TelephonyManager tMgr = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
                mPhoneNumber = tMgr.getLine1Number();
                Log.d("Main Activity", "Phone number: " + mPhoneNumber);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Main Activity", "Could not retrieve user phone number");
            }

        }

        /**
         * Check if READ_PHONE_STATE permission is set. Request user if not.
         */
        if(PERMISSION_READ_PHONE_STATE != PackageManager.PERMISSION_GRANTED) {

            // Does the the user needs an explanation?
            // check if app has requested this permission previously and the user denied the request
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_PHONE_STATE))
                createAndShowDialog("This app needs your phone number to validate the issue",
                        "Phone Number Needed");

            // request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
        }
    }


    /**
     * sets mission critical variables to appropriate states
     * @param savedInstanceState
     */
    private void updateDataFromBundle(Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_SAVED_BUNDLE)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // mCurrentLocationis not null.
                mLastLocation = savedInstanceState.getParcelable(LOCATION_SAVED_BUNDLE);
            }

            if (savedInstanceState.keySet().contains(PHOTO_SAVED_BUNDLE)) {
                mPhotoFileExists = savedInstanceState.getBoolean(PHOTO_SAVED_BUNDLE);
            }

        }
    }


    @Override
    protected void onStart() {
        super.onStart();
    }


    /**
     * Initializes the onStop method for activity
     */
    @Override
    protected void onStop() {
        // disconnect Google API client
        mGoogleApiClient.disconnect();
        super.onStop();
    }


    /**
     * When user switches to another app, don't want
     * location updates to keep coming in.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if(mGoogleApiClient.isConnected())
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        hasRunOnce = true;
    }


    /**
     * When the user comes back in, time to start
     * location updates
     */
    @Override
    protected void onResume() {
        super.onResume();
        if(mPhotoFileExists) {
            if (mGoogleApiClient != null && !mGoogleApiClient.isConnected() && hasRunOnce) {
                mGoogleApiClient.connect();
            }
        }
    }


    /**
     * Ensure that my last location is always accessible
     * @param savedInstanceState
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(LOCATION_SAVED_BUNDLE, mLastLocation);
        savedInstanceState.putBoolean(PHOTO_SAVED_BUNDLE, mPhotoFileExists);
        super.onSaveInstanceState(savedInstanceState);
    }


    /**
     * Authenticates with the desired login provider. Also caches the token.
     *
     * If a local token cache is detected, the token cache is used instead of an actual
     * login unless bRefresh is set to true forcing a refresh.
     *
     * @param bRefreshCache
     *            Indicates whether to force a token refresh.
     */
    private void authenticate(boolean bRefreshCache) {

        bAuthenticating = true;

        if (bRefreshCache || !loadUserTokenCache(mClient)) {
            // if for some reason, authentication was not available,
            // user needs to be prompted to login again

            // create dialog to prompt user to choose authentication method
            AlertDialog.Builder authAlertDialog =
                    new AlertDialog.Builder(this);

            authAlertDialog.setTitle(AUTH_DIALOG_TITLE);

            authAlertDialog.setItems(AUTH_SERVERS,
                    new DialogInterface.OnClickListener() {

                        MobileServiceAuthenticationProvider selectedProvider =
                                MobileServiceAuthenticationProvider.Google;

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                // Google
                                case 0:
                                    selectedProvider =
                                            MobileServiceAuthenticationProvider.Google;
                                    break;
                                // Facebook
                                case 1:
                                    selectedProvider =
                                            MobileServiceAuthenticationProvider.Facebook;
                                    break;
                                // Microsoft
                                case 2:
                                    selectedProvider =
                                            MobileServiceAuthenticationProvider.MicrosoftAccount;
                                    break;
                                // Setting Facebook as fallback
                                default:
                                    selectedProvider =
                                            MobileServiceAuthenticationProvider.Facebook;

                            }

                            // New login using the provider and update the token cache.
                            mClient.login(selectedProvider,
                                    new UserAuthenticationCallback() {
                                        @Override
                                        public void onCompleted(MobileServiceUser user,
                                                                Exception exception, ServiceFilterResponse response) {

                                            synchronized (mAuthenticationLock) {
                                                if (exception == null) {
                                                    cacheUserToken(mClient.getCurrentUser());
                                                    createTable();
                                                } else {
                                                    createAndShowDialog(exception.getMessage(), "Login Error");
                                                }
                                                // request phone number once auth is done
                                                retrievePhoneNumber();

                                                bAuthenticating = false;
                                                mAuthenticationLock.notifyAll();
                                            }
                                        }
                                    });
                        }
                    });

            //
            authAlertDialog.setCancelable(!OAUTH_REQUIRED);

            authAlertDialog.show();

        } else {
            // Other threads may be blocked waiting to be notified when
            // authentication is complete.
            synchronized (mAuthenticationLock) {
                bAuthenticating = false;
                mAuthenticationLock.notifyAll();
            }
            createTable();
        }
    }


    /**
     * Detects if authentication is in progress and waits for it to complete.
     * Returns true if authentication was detected as in progress. False otherwise.
     */
    public boolean detectAndWaitForAuthentication() {
        boolean detected = false;
        synchronized (mAuthenticationLock) {
            do {
                if (bAuthenticating == true)
                    detected = true;
                try {
                    mAuthenticationLock.wait(1000);
                } catch (InterruptedException e) {
                }
            }
            while (bAuthenticating == true);
        }
        if (bAuthenticating == true)
            return true;

        return detected;
    }


    /**
     * Waits for authentication to complete then adds or updates the token
     * in the X-ZUMO-AUTH request header.
     *
     * @param request
     *            The request that receives the updated token.
     */
    private void waitAndUpdateRequestToken(ServiceFilterRequest request) {
        MobileServiceUser user = null;
        if (detectAndWaitForAuthentication()) {
            user = mClient.getCurrentUser();
            if (user != null) {
                request.removeHeader("X-ZUMO-AUTH");
                request.addHeader("X-ZUMO-AUTH", user.getAuthenticationToken());
            }
        }
    }


    /**
     * Initialize the table of entries entered by this user
     */
    private void createTable() {

        // Get the Mobile Service Table instance to use
        mToDoTable = mClient.getTable(ToDoItem.class);

        mTextNewToDo = (EditText) findViewById(R.id.textNewToDo);

        // Create an adapter to bind the items with the view
        mAdapter = new ToDoItemAdapter(this, R.layout.row_list_to_do);
        ListView listViewToDo = (ListView) findViewById(R.id.listViewToDo);
        listViewToDo.setAdapter(mAdapter);

        // Load the items from the Mobile Service
        refreshItemsFromTable();
    }


    /**
     * Initializes the activity menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }


    /**
     * Select an option from the menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            refreshItemsFromTable();
        }

        return true;
    }


    /**
     * !!!FOR DEBUGGING PURPOSES ONLY!!!
     * Mark an item as completed
     *
     * @param item
     *            The item to mark
     */
    public void checkItem(final ToDoItem item) {
        if (mClient == null) {
            return;
        }

        // Set the item as completed and update it in the table
        item.setComplete(true);

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {

                    checkItemInTable(item);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (item.isComplete()) {
                                mAdapter.remove(item);
                            }
                        }
                    });
                } catch (final Exception e) {
                    createAndShowDialogFromTask(e, "Error");
                }

                return null;
            }
        };

        runAsyncTask(task);

    }


    /**
     * Mark an item as completed in the Mobile Service Table
     *
     * @param item
     *            The item to mark
     */
    public void checkItemInTable(ToDoItem item) throws ExecutionException, InterruptedException {
        mToDoTable.update(item).get();
    }


    /**
     * Add a new item
     *
     * @param view
     *            The view that originated the call
     */
    public void uploadPhoto(View view) {
        if (mClient == null) {
            return;
        }

        // check if file is not readable for some reason
        if(new File(mPhotoFileUri.getPath()).length() == 0)
            createAndShowDialog("Upload failed", "File Access Error");

//        Bitmap bitmap = BitmapFactory.decodeFile(mPhotoFileUri.getPath());
//        File newImageFile = null;
//        try {
//            newImageFile = createImageFile();
//            FileOutputStream newImageFileStream = null;
//            try {
//                newImageFileStream = new FileOutputStream(newImageFile);
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 10, newImageFileStream); // bmp is your Bitmap instance
//                // PNG is a lossless format, the compression factor (100) is ignored
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                try {
//                    if (newImageFileStream != null) {
//                        newImageFileStream.close();
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        // deactivate upload button
        toggleUploadOff();

        // Create a new item
        final ToDoItem item = new ToDoItem();

        // if user doesn't add description
        String text = mTextNewToDo.getText().toString();
        if(text.length() < 0) {
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
                    final ToDoItem entity = addItemInTable(item);

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

                        blobFromSASCredential.uploadFromFile(mPhotoFileUri.getPath());
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!entity.isComplete()) {
                                mAdapter.add(entity);
                            }
                        }
                    });
                } catch (final Exception e) {
                    createAndShowDialogFromTask(e, "Error");
                }
                return null;
            }
        };

        runAsyncTask(task);

        mTextNewToDo.setText("");
    }


    /**
     * Add an item to the Mobile Service Table
     *
     * @param item
     *            The item to Add
     */
    public ToDoItem addItemInTable(ToDoItem item) throws ExecutionException, InterruptedException {
        ToDoItem entity = mToDoTable.insert(item).get();
        return entity;
    }


    /**
     * Refresh the list with the items in the Table
     */
    private void refreshItemsFromTable() {

        // Get the items that weren't marked as completed and add them in the
        // adapter

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                try {
                    final List<ToDoItem> results = refreshItemsFromMobileServiceTable();

                    //Offline Sync
                    //final List<ToDoItem> results = refreshItemsFromMobileServiceTableSyncTable();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.clear();

                            for (ToDoItem item : results) {
                                mAdapter.add(item);
                            }
                        }
                    });
                } catch (final Exception e) {
                    createAndShowDialogFromTask(e, "Error");
                }

                return null;
            }
        };

        runAsyncTask(task);
    }


    /**
     * This method stores the user id and token in a preference
     * file that is marked private. This should protect access to
     * the cache so that other apps on the device do not have access
     * to the token because the preference is sandboxed for the app.
     * However, if someone gains access to the device, it is
     * possible that they may gain access to the token cache
     * through other means.
     * @param user
     */
    private void cacheUserToken(MobileServiceUser user) {
        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(USERIDPREF, user.getUserId());
        editor.putString(TOKENPREF, user.getAuthenticationToken());
        editor.commit();
    }


    /**
     * This method loads a user token from the preference file.
     * @param client
     * @return
     */
    private boolean loadUserTokenCache(MobileServiceClient client) {
        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        String userId = prefs.getString(USERIDPREF, "undefined");
        if (userId == "undefined")
            return false;
        String token = prefs.getString(TOKENPREF, "undefined");
        if (token == "undefined")
            return false;

        MobileServiceUser user = new MobileServiceUser(userId);
        user.setAuthenticationToken(token);
        client.setCurrentUser(user);

        return true;
    }


    /**
     * Refresh the list with the items in the Mobile Service Table
     */
    private List<ToDoItem> refreshItemsFromMobileServiceTable() throws ExecutionException, InterruptedException {
        return mToDoTable.where().field("complete").
                eq(val(false)).execute().get();
    }


    /**
     * Creates a dialog and shows it
     *
     * @param exception
     *            The exception to show in the dialog
     * @param title
     *            The dialog title
     */
    private void createAndShowDialogFromTask(final Exception exception, String title) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                createAndShowDialog(exception, "Error");
            }
        });
    }


    /**
     * Creates a dialog and shows it
     *
     * @param exception
     *            The exception to show in the dialog
     * @param title
     *            The dialog title
     */
    private void createAndShowDialog(Exception exception, String title) {
        Throwable ex = exception;
        if (exception.getCause() != null) {
            ex = exception.getCause();
        }
        createAndShowDialog(ex.getMessage(), title);
    }


    /**
     * Creates a dialog and shows it
     *
     * @param message
     *            The dialog message
     * @param title
     *            The dialog title
     */
    private void createAndShowDialog(final String message, final String title) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    }


    /**
     * Run an ASync task on the corresponding executor
     * @param task
     * @return
     */
    private AsyncTask<Void, Void, Void> runAsyncTask(AsyncTask<Void, Void, Void> task) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            return task.execute();
        }
    }


    /**
     * For Google API Client
     * @param bundle
     */
    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            /**
             *  recheck if permissions were set
             *  if not, ask user for permission
             */

            wereLocationServicesPermitted();

            mLastLatitude = "Location not permitted";
            mLastLongitude = "Location not permitted";
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        // refresh the location
        startLocationUpdates();

        if (mLastLocation != null) {
            mLastLatitude = String.valueOf(mLastLocation.getLatitude());
            mLastLongitude = String.valueOf(mLastLocation.getLongitude());
            return;
        } else {
            // location data not available for some reason since mLastLocation is null
            mLastLatitude = DEFAULT_LOCATION_NOT_FOUND;
            mLastLongitude = DEFAULT_LOCATION_NOT_FOUND;
        }
    }


    /**
     * For Google API Client
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {
        createAndShowDialog("The connection isn't live anymore", "Connection Suspended");
    }


    /**
     * For Google API Client
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        createAndShowDialog("Something is wrong with the connection", "Connection Failed");
    }


    /**
     * Callback for when user responds to dialog for 'dangerous permissions'
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            // fine location
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    PERMISSION_ACCESS_FINE_LOCATION = ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION);

                    // request location update
                    createLocationRequest();

                    // activate upload button and change take photo
                    // button functionality
                    toggleUploadOn();

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    PERMISSION_ACCESS_COARSE_LOCATION = PackageManager.PERMISSION_DENIED;

//                    createAndShowDialog("Need access to location data to understand " +
//                            "where the photo was captured", "Permission not found");

                    Toast.makeText(getApplicationContext(), "Access to GPS needed for photo " +
                            "location", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // coarse location
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    PERMISSION_ACCESS_COARSE_LOCATION = ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION);

                    // request location update
                    createLocationRequest();

                    // activate upload button and change take photo
                    // button functionality
                    toggleUploadOn();

                    // first time case: got permission!
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    PERMISSION_ACCESS_COARSE_LOCATION = PackageManager.PERMISSION_DENIED;

//                    createAndShowDialog("Need access to location data to understand " +
//                            "where the photo was captured", "Permission not found");

                    Toast.makeText(getApplicationContext(), "Access to GPS needed for photo " +
                            "location", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // write to external storage
            case MY_PERMISSIONS_REQUEST_ACCESS_EXTERNAL_STORAGE: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    PERMISSION_ACCESS_EXTERNAL_STORAGE = ContextCompat.checkSelfPermission(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE);

                    // first time case: got permission! take a photo
                    try {
                        // since we must have EXTERNAL_STORAGE permission, mPhotoFile should
                        // not be null
                        mPhotoFile = createImageFile();
                        takePhoto(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    PERMISSION_ACCESS_EXTERNAL_STORAGE = PackageManager.PERMISSION_DENIED;

                    createAndShowDialog("Need access to external storage to store captured " +
                            "photo", "Permission not found");

                    Toast.makeText(getApplicationContext(), "Need access to external storage " +
                            "to store captured photo", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            case MY_PERMISSIONS_REQUEST_READ_PHONE_STATE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    PERMISSION_READ_PHONE_STATE = ContextCompat.checkSelfPermission(this,
                            Manifest.permission.READ_PHONE_STATE);

                    retrievePhoneNumber();

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    PERMISSION_READ_PHONE_STATE = PackageManager.PERMISSION_DENIED;

                    createAndShowDialog("Need phone number to validate", "Permission not found");

                    Toast.makeText(getApplicationContext(), "Need phone number to " +
                            "validate", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }


    /**
     * Checks if the application had Location permissions set.
     * If not, the user is requested to set the permissions.
     * This functions returns false if the permissions are not set,
     * true otherwise
     */
    private boolean wereLocationServicesPermitted() {
        // Fine Location Permission
        PERMISSION_ACCESS_FINE_LOCATION = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        // Coarse Location Permission
        PERMISSION_ACCESS_COARSE_LOCATION = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        /**
         * Check if both permissions were set
         * If so, return true
         */
        if (PERMISSION_ACCESS_FINE_LOCATION == PackageManager.PERMISSION_GRANTED
                && PERMISSION_ACCESS_COARSE_LOCATION == PackageManager.PERMISSION_GRANTED)
            return true;


        /**
         * Check if FINE_LOCATION permission is set. Request user if not.
         */
        if (PERMISSION_ACCESS_FINE_LOCATION != PackageManager.PERMISSION_GRANTED) {

            // Does the the user needs an explanation?
            // check if app has requested this permission previously and the user denied the request
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION))
                createAndShowDialog("This app needs access to your device\'s accurate GPS" +
                        "location to know exactly where this photo was taken" +
                        "", "Accurate Location Required");

            // request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }


        /**
         * Check if COARSE_LOCATION permission is set. Request user if not.
         */
        if (PERMISSION_ACCESS_COARSE_LOCATION != PackageManager.PERMISSION_GRANTED) {

            // Does the the user needs an explanation?
            // check if app has requested this permission previously and the user denied the request
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION))
                createAndShowDialog("This app needs access to your device\'s approximate GPS" +
                        "location to know which region this photo was taken in" +
                        "", "Approximate Location Required");

            // request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }

        return false;
    }


    /**
     * Callback that hits everytime a request for new location
     * goes through. mLastLocation object is updated. So are the
     * strings containing latitude and longitude.
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;

        // can check for updated time here
        // mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

        // update latitude and longitude
        mLastLatitude = String.valueOf(mLastLocation.getLatitude());
        mLastLongitude = String.valueOf(mLastLocation.getLongitude());
    }


    /**
     * The ProgressFilter class renders a progress bar on the screen during the time the App is waiting for the response of a previous request.
     * the filter shows the progress bar on the beginning of the request, and hides it when the response arrived.
     */
    private class ProgressFilter implements ServiceFilter {
        @Override
        public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {

            final SettableFuture<ServiceFilterResponse> resultFuture = SettableFuture.create();

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (mProgressBar != null) mProgressBar.setVisibility(ProgressBar.VISIBLE);
                }
            });

            ListenableFuture<ServiceFilterResponse> future = nextServiceFilterCallback.onNext(request);

            Futures.addCallback(future, new FutureCallback<ServiceFilterResponse>() {
                @Override
                public void onFailure(Throwable e) {
                    resultFuture.setException(e);
                }

                @Override
                public void onSuccess(ServiceFilterResponse response) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (mProgressBar != null) mProgressBar.setVisibility(ProgressBar.GONE);
                        }
                    });

                    resultFuture.set(response);
                }
            });

            return resultFuture;
        }
    }


    // Run an Intent to start up the Android camera
    public void takePicture(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            try {
                if (haveWritePermissions()) {
                    mPhotoFile = createImageFile();
                    takePhoto(takePictureIntent);
                }
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(getApplicationContext(), "Could not create file to store photo", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(getApplicationContext(), "No camera activity to handle intent", Toast.LENGTH_SHORT).show();
        }
    }


    private void takePhoto(Intent takePictureIntent) {
        // Continue only if the File was successfully created
        if (mPhotoFile != null) {
            mPhotoFileUri = Uri.fromFile(mPhotoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoFileUri);
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

            // need location where picture was taken
            // connect Google API client if not connected
            if (!mGoogleApiClient.isConnected())
                mGoogleApiClient.connect();

            // location data is key
            // if location data is not available, cannot let user upload photo
            if (wereLocationServicesPermitted()) {
                // request location update
//                createLocationRequest();

                // activate upload button and change take photo
                // button functionality
                toggleUploadOn();
            }
        }
    }

    /**
     * Initialize the mLocationRequest object to start sampling
     * Checks to see if location settings are handled, and take
     * appropriate actions.
     */
    protected void createLocationRequest() {
        // create a location request object
        if (mLocationRequest == null) {
            mLocationRequest = new LocationRequest();

            /**
             * The priority of PRIORITY_HIGH_ACCURACY, combined
             * with the ACCESS_FINE_LOCATION permission setting
             * defined in the app manifest, and a fast update
             * interval of 5000 milliseconds (5 seconds), causes
             * the fused location provider to return location
             * updates that are accurate to within a few feet.
             * This approach is appropriate for mapping apps
             * that display the location in real time.
             */
            // rate at which location updates will be received
            mLocationRequest.setInterval(1000);

            // location updates will not be received at a rate faster than this
            mLocationRequest.setFastestInterval(5000);

            // get most precise location possible
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }

        // get current location settings
        LocationSettingsRequest.Builder locationRequestBuilder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        // check if location settings are satisfied
        final PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        locationRequestBuilder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                final LocationSettingsStates states = locationSettingsResult.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        startLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    ToDoActivity.this,
                                    LOCATION_REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        // ...
                        break;
                }
            }
        });
    }


    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            wereLocationServicesPermitted();
            return;
        }
        if(mLocationRequest == null)
            // create
            createLocationRequest();
        else
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, (LocationListener) this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(resultCode) {
            case RESULT_OK: {
                mGoogleApiClient.connect();
                createLocationRequest();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    // Create a File object for storing the photo
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getApplicationContext().getExternalFilesDir(null);
                //Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                //getApplicationContext().getFilesDir();


        mPhotoFileExists = true;

        // check if access to external storage exists
        // if returned true, return the new file
        // if returned false, user could have been requested for access
        // so, need to check if permission is available now
        // if still not available, return null

        if(haveWritePermissions())
            return File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
        return null;
    }

    @Override
    public void onAsyncTaskComplete(Bitmap bmp) {
        // pipe new bitmap to file
        // since this requires calling compress which can take
        // some time, moving to an async task
    }

    @Override
    public void onAsyncTaskComplete(File comprssdFile) {
        // compare compressed file to original file
    }

    private boolean haveWritePermissions() {
        // External storage write permission
        PERMISSION_ACCESS_EXTERNAL_STORAGE = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

       /**
         * Check if permissions were set
         * If so, return true
         */
        if(PERMISSION_ACCESS_EXTERNAL_STORAGE == PackageManager.PERMISSION_GRANTED)
            return true;


        /**
         * Check if EXTERNAL_STORAGE permission is set. Request user if not.
         */
        if(PERMISSION_ACCESS_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {

            // Does the the user needs an explanation?
            // check if app has requested this permission previously and the user denied the request
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE))
                createAndShowDialog("This app needs access to your device\'s storage " +
                        "to store the photo","Access to Storage required");

            // request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_ACCESS_EXTERNAL_STORAGE);
        }

        return PERMISSION_ACCESS_EXTERNAL_STORAGE == PackageManager.PERMISSION_GRANTED;
    }


    /**
     * Changes button functionality when photo is available for upload
     * Upload button activated
     * Take photo button lets user retake photo
     */
    private void toggleUploadOn() {
        // activate upload button
        setUploadButton(true);

        // change preview button text to let user capture new photo
        // reset take photo button text
        Button previewButton = (Button) findViewById(R.id.buttonPreview);
        previewButton.setText(R.string.reset_preview_button_text);
    }


    /**
     * Changes button functionality when photo is available for upload
     * Upload button activated
     * Take photo button lets user retake photo
     */
    private void toggleUploadOff() {
        // deactivate upload button
        setUploadButton(false);

        // change preview button text to default
        Button previewButton = (Button) findViewById(R.id.buttonPreview);
        previewButton.setText(R.string.preview_button_text);
    }


    /**
     * Sets the clickable and enabled states of the upload button
     * @param state boolean state to turn upload button clickable and enable to on/off
     */
    private void setUploadButton(boolean state) {
        Button uploadButton = (Button) findViewById(R.id.buttonUpload);
        uploadButton.setClickable(state);
        uploadButton.setEnabled(state);
    }


    /**
     * The RefreshTokenCacheFilter class filters responses for HTTP status code 401.
     * When 401 is encountered, the filter calls the authenticate method on the
     * UI thread. Out going requests and retries are blocked during authentication.
     * Once authentication is complete, the token cache is updated and
     * any blocked request will receive the X-ZUMO-AUTH header added or updated to
     * that request.
     */
    private class RefreshTokenCacheFilter implements ServiceFilter {

        AtomicBoolean mAtomicAuthenticatingFlag = new AtomicBoolean();

        @Override
        public ListenableFuture<ServiceFilterResponse> handleRequest(
                final ServiceFilterRequest request,
                final NextServiceFilterCallback nextServiceFilterCallback
        )
        {
            // In this example, if authentication is already in progress we block the request
            // until authentication is complete to avoid unnecessary authentications as
            // a result of HTTP status code 401.
            // If authentication was detected, add the token to the request.
            waitAndUpdateRequestToken(request);

            // Send the request down the filter chain
            // retrying up to 5 times on 401 response codes.
            ListenableFuture<ServiceFilterResponse> future = null;
            ServiceFilterResponse response = null;
            int responseCode = 401;
            for (int i = 0; (i < 5 ) && (responseCode == 401); i++)
            {
                future = nextServiceFilterCallback.onNext(request);
                try {
                    response = future.get();
                    responseCode = response.getStatus().getStatusCode();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    if (e.getCause().getClass() == MobileServiceException.class)
                    {
                        MobileServiceException mEx = (MobileServiceException) e.getCause();
                        responseCode = mEx.getResponse().getStatus().getStatusCode();
                        if (responseCode == 401)
                        {
                            // Two simultaneous requests from independent threads could get HTTP status 401.
                            // Protecting against that right here so multiple authentication requests are
                            // not setup to run on the UI thread.
                            // We only want to authenticate once. Requests should just wait and retry
                            // with the new token.
                            if (mAtomicAuthenticatingFlag.compareAndSet(false, true))
                            {
                                // Authenticate on UI thread
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Force a token refresh during authentication.
                                        authenticate(true);
                                    }
                                });
                            }

                            // Wait for authentication to complete then update the token in the request.
                            waitAndUpdateRequestToken(request);
                            mAtomicAuthenticatingFlag.set(false);
                        }
                    }
                }
            }
            return future;
        }
    }

}