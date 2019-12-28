package com.cenah.cameraservice;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;

import static com.cenah.cameraservice.Worker.SHOW_RESULT;

public class MainActivity extends AppCompatActivity implements WorkerResultReceiver.Receiver {

    WorkerResultReceiver mWorkerResultReceiver;
    TextView mTextView;
    private static final int REQUEST_PERMISSIONS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                checkAllPermessions();
               // Intent serviceIntent = new Intent(MainActivity.this, ImageService.class);
              //  ImageService.enqueueWork(MainActivity.this, serviceIntent);



            }
        });


    }

    private void startSerive() {
        Date currentTime = Calendar.getInstance().getTime();
        long temp = currentTime.getTime()/1000;
        new AppSharePref(getApplicationContext()).saveSharedInfo(new FileModel(null,null, temp));
        mWorkerResultReceiver = new WorkerResultReceiver(new Handler());
        mWorkerResultReceiver.setReceiver(MainActivity.this);
        mTextView = findViewById(R.id.tv_data);
        showDataFromBackground(MainActivity.this, mWorkerResultReceiver);
    }

    public void checkAllPermessions(){

        if ((ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            if ((ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) && (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE))) {

            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERMISSIONS);
            }
        }else {
            Log.e("Else","Else");
            startSerive();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults.length > 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        startSerive();
                    } else {
                        Toast.makeText(MainActivity.this, "The app was not allowed to read or write to your storage. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    private void showDataFromBackground(MainActivity mainActivity, WorkerResultReceiver mWorkerResultReceiver) {
        Worker.enqueueWork(mainActivity, mWorkerResultReceiver);
    }

    public void showData(String data) {
        mTextView.setText(String.format("%s\n%s", mTextView.getText(), data));
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultCode == SHOW_RESULT) {
            if (resultData != null) {
                showData(resultData.getString("data"));
            }
        }
    }
}
