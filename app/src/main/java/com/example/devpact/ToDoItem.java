package com.example.devpact;

/**
 * Represents an item in a ToDo list
 */
public class ToDoItem {

    /**
     * Item text
     */
    @com.google.gson.annotations.SerializedName("text")
    private String mText;

    /**
     * Item Id
     */
    @com.google.gson.annotations.SerializedName("id")
    private String mId;

    /**
     * Phone number
     */
    @com.google.gson.annotations.SerializedName("phoneNumber")
    private String mPhoneNumber;


    /**
     * Latitude
     */
    @com.google.gson.annotations.SerializedName("latitude")
    private String mLatitude;

    /**
     * Longitude
     */
    @com.google.gson.annotations.SerializedName("longitude")
    private String mLongitude;


    /**
     * Returns the latitude
     */
    public String getLatitude() {
        return mLatitude;
    }

    /**
     * Sets the latitude
     * @param latitude
     *      latitude coordinate
     */
    public final void setLatitude(String latitude) {
        this.mLatitude = latitude;
    }

    /**
     * Sets the longitude
     * @param longitude
     *      longitude coordinate
     */
    public final void setLongitude(String longitude) {
        this.mLongitude = longitude;
    }

    /**
     * Returns the longitude
     */
    public String getLongitude() {
        return mLongitude;
    }

    /**
     * Returns the phone number
     */
    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    /**
     * Sets the item Phone number
     *
     * @param PhoneNumber
     *            PhoneNumber to set
     */
    public final void setPhoneNumber(String PhoneNumber) {
        mPhoneNumber = PhoneNumber;
    }

    /**
     * Indicates if the item is completed
     */
    @com.google.gson.annotations.SerializedName("complete")
    private boolean mComplete;

    /**
     *  imageUri - points to location in storage where photo will go
     */
    @com.google.gson.annotations.SerializedName("imageUri")
    private String mImageUri;

    /**
     * Returns the item ImageUri
     */
    public String getImageUri() {
        return mImageUri;
    }

    /**
     * Sets the item ImageUri
     *
     * @param ImageUri
     *            Uri to set
     */
    public final void setImageUri(String ImageUri) {
        mImageUri = ImageUri;
    }

    /**
     * ContainerName - like a directory, holds blobs
     */
    @com.google.gson.annotations.SerializedName("containerName")
    private String mContainerName;

    /**
     * Returns the item ContainerName
     */
    public String getContainerName() {
        return mContainerName;
    }

    /**
     * Sets the item ContainerName
     *
     * @param ContainerName
     *            Uri to set
     */
    public final void setContainerName(String ContainerName) {
        mContainerName = ContainerName;
    }

    /**
     *  ResourceName
     */
    @com.google.gson.annotations.SerializedName("resourceName")
    private String mResourceName;

    /**
     * Returns the item ResourceName
     */
    public String getResourceName() {
        return mResourceName;
    }

    /**
     * Sets the item ResourceName
     *
     * @param ResourceName
     *            Uri to set
     */
    public final void setResourceName(String ResourceName) {
        mResourceName = ResourceName;
    }

    /**
     *  SasQueryString - permission to write to storage
     */
    @com.google.gson.annotations.SerializedName("sasQueryString")
    private String mSasQueryString;

    /**
     * Returns the item SasQueryString
     */
    public String getSasQueryString() {
        return mSasQueryString;
    }

    /**
     * Sets the item SasQueryString
     *
     * @param SasQueryString
     *            Uri to set
     */
    public final void setSasQueryString(String SasQueryString) {
        mSasQueryString = SasQueryString;
    }

    /**
     * ToDoItem constructor
     */
    public ToDoItem() {
        mContainerName = "";
        mResourceName = "";
        mImageUri = "";
        mSasQueryString = "";
    }

    @Override
    public String toString() {
        return getText();
    }

    /**
     * Initializes a new ToDoItem
     *
     * @param text
     *            The item text
     * @param id
     *            The item id
     */
    public ToDoItem(String text,
                    String id,
                    String containerName,
                    String resourceName,
                    String imageUri,
                    String sasQueryString) {
        this.setText(text);
        this.setId(id);
        this.setContainerName(containerName);
        this.setResourceName(resourceName);
        this.setImageUri(imageUri);
        this.setSasQueryString(sasQueryString);
    }

    /**
     * Returns the item text
     */
    public String getText() {
        return mText;
    }

    /**
     * Sets the item text
     *
     * @param text
     *            text to set
     */
    public final void setText(String text) {
        mText = text;
    }

    /**
     * Returns the item id
     */
    public String getId() {
        return mId;
    }

    /**
     * Sets the item id
     *
     * @param id
     *            id to set
     */
    public final void setId(String id) {
        mId = id;
    }

    /**
     * Indicates if the item is marked as completed
     */
    public boolean isComplete() {
        return mComplete;
    }

    /**
     * Marks the item as completed or incompleted
     */
    public void setComplete(boolean complete) {
        mComplete = complete;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ToDoItem && ((ToDoItem) o).mId == mId;
    }
}