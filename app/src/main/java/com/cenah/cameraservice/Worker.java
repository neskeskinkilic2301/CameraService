package com.cenah.cameraservice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.JobIntentService;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;

public class Worker extends JobIntentService {

    private DatabaseReference reff;


    private static final String TAG = "Worker";
    public static final String RECEIVER = "receiver";
    public static final int SHOW_RESULT = 123;
    /**
     * Result receiver object to send results
     */
    private ResultReceiver mResultReceiver;
    /**
     * Unique job ID for this service.
     */
    static final int DOWNLOAD_JOB_ID = 1000;
    /**
     * Actions download
     */
    private static final String ACTION_DOWNLOAD = "action.DOWNLOAD_DATA";

    /**
     * Convenience method for enqueuing work in to this service.
     */

    private long maxId;
    public static void enqueueWork(Context context, WorkerResultReceiver workerResultReceiver) {
        Intent intent = new Intent(context, Worker.class);
        intent.putExtra(RECEIVER, workerResultReceiver);
        intent.setAction(ACTION_DOWNLOAD);
        enqueueWork(context, Worker.class, DOWNLOAD_JOB_ID, intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @SuppressLint("DefaultLocale")
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        reff = FirebaseDatabase.getInstance().getReference().child("Notification");
        reff.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                    maxId = (dataSnapshot.getChildrenCount());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        Log.d(TAG, "onHandleWork() called with: intent = [" + intent + "]");
        if (intent.getAction() != null) {


            if (ACTION_DOWNLOAD.equals(intent.getAction())) {
                mResultReceiver = intent.getParcelableExtra(RECEIVER);
                while (true) {
                    try {
                        uploadImage(getAllShownImagesPath());
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    private ArrayList<FileModel> getAllShownImagesPath() {
        Uri uri;
        Cursor cursor;
        int column_index_data;
        ArrayList<FileModel> listOfAllImages = new ArrayList<>();

        String absolutePathOfImage;
        uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.MediaColumns.DATE_ADDED};

        cursor = getBaseContext().getContentResolver().query(uri, projection, null, null, MediaStore.Images.Media.DATE_ADDED);
        //createdDate = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
        column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

        long lastDate = new AppSharePref(getBaseContext()).getSharedInfo().getDate();
        while (cursor.moveToNext()) {
            absolutePathOfImage = cursor.getString(column_index_data);
            long date = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED));
            if (date > lastDate)
                listOfAllImages.add(new FileModel(absolutePathOfImage.substring(absolutePathOfImage.lastIndexOf("/") + 1), absolutePathOfImage, date));
        }
        if (!listOfAllImages.isEmpty())
            new AppSharePref(getBaseContext()).saveSharedInfo(listOfAllImages.get(listOfAllImages.size() - 1));
        return listOfAllImages;
    }

    private void uploadImage(ArrayList<FileModel> allShownImagesPath) {

        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        if(allShownImagesPath.isEmpty()){
            Bundle bundle = new Bundle();
            bundle.putString("data", "No new image");
            mResultReceiver.send(SHOW_RESULT, bundle);
        }
        for (FileModel file : allShownImagesPath) {
            final StorageReference imageFolder = storageReference.child("images/" + file.getName() + "" + Calendar.getInstance().getTime().getTime());
            Uri url = Uri.fromFile(new File(file.getPath()));
            imageFolder.putFile(url).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    Bundle bundle = new Bundle();
                    bundle.putString("data", "uplaoded");
                    mResultReceiver.send(SHOW_RESULT, bundle);

                    imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            // set value for cat if image uploaded and we can get download link
                            Notification notification =new Notification((int) maxId+1,"Camera",uri.toString());
                            reff.child(String.valueOf(maxId+1)).setValue(notification);



                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                    Bundle bundle = new Bundle();
                    bundle.putString("data", e.getMessage());
                    mResultReceiver.send(SHOW_RESULT, bundle);

                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {


                }
            });
        }


    }




}