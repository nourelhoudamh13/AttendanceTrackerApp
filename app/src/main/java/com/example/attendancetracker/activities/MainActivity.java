package com.example.attendancetracker.activities;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.attendancetracker.BackgroundTask;
import com.example.attendancetracker.R;
import com.example.attendancetracker.scheduler.MJobScheduler;
import com.example.attendancetracker.NetworkConnection;
import com.example.attendancetracker.scheduler.UserData;
import com.example.attendancetracker.reciever.ConnectionCallback;
import com.example.attendancetracker.reciever.NetworkChangeReceiver;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements ConnectionCallback {

    private TextView mDate;
    private TextView mWifiStatuses;
    private TextView mCheckedOut;
    private TextView mCheckedIn;
    private TextView mLeftAt;

    private int mConnectedColor;
    private int mDisconnectedColor;

    private int mFlag = 2;
    int mAsyncFlag;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;

    private NetworkConnection networkConnection;

    private Resources mRes;

    private NetworkChangeReceiver receiver; // The BroadcastReceiver that tracks network connectivity changes.

    public Map map;

    public String userId;

    private static final int JOB_ID = 101;
    private JobScheduler jobScheduler;
    private JobInfo jobInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //views
        initializeUI();

        //Resources
        resources();

        //get date
        getDate();

        //a reference to your Firebase node
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mDatabaseReference = mFirebaseDatabase.getReference();

        //network
        networkConnection = new NetworkConnection();
        mAsyncFlag = networkConnection.networkStatus(this);

        //AsyncTAsk
        //startAsyncTAsk();

        //JobService
        startJobService();
         Toast.makeText(getApplicationContext(), "JobScheduler-MainActivity", Toast.LENGTH_SHORT).show();

        //updateUI(mFlag);

    }

    @Override
    protected void onResume() {
        super.onResume();
        //Registers BroadcastReceiver to track network connection changes.
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkChangeReceiver(this);
        this.registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiver != null) {
            this.unregisterReceiver(receiver);

        }

    }

    //TextViews
    private void initializeUI() {

        mWifiStatuses = findViewById(R.id.text_wifi_status);
        mCheckedIn = findViewById(R.id.txt_checked_in);
        mCheckedOut = findViewById(R.id.txt_checked_out);
        mLeftAt = findViewById(R.id.txt_left_at);
        mDate = findViewById(R.id.text_date);
    }

    private void resources() {
        mRes = getResources();
        mConnectedColor = mRes.getColor(R.color.colorGreen);
        mDisconnectedColor = mRes.getColor(R.color.colorRed);
    }

    private void getDate() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sDfr = new SimpleDateFormat("yyyy. MM. dd ");
        String strDate = sDfr.format(calendar.getTime());
        mDate.setText(strDate);
        mDate.setTextColor(Color.BLUE);
    }

    private void startAsyncTAsk() {
        BackgroundTask backgroundTask = new BackgroundTask(this);
        backgroundTask.execute(mAsyncFlag);
    }

    private void startJobService() {
        ComponentName serviceComponent = new ComponentName(this, MJobScheduler.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, serviceComponent);

        builder.setPeriodic(6000);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE);
        builder.setPersisted(true);

        jobInfo = builder.build();
        jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(jobInfo);
        Toast.makeText(getApplicationContext(), "JobScheduler", Toast.LENGTH_SHORT).show();
//        Log.v("MainActivity", "onCreate: JobScheduler");
    }

    private void getTimeStamp() {
        map = new HashMap();
        map.put("Time", ServerValue.TIMESTAMP);
    }

    private String getUserId() {
        if (TextUtils.isEmpty(userId)) {
            userId = mDatabaseReference.push().getKey();
        }
        return userId;
    }

    private void createUserData(Map arrivalTime, Map departureTime) {
        UserData user = new UserData(userId ,arrivalTime, departureTime);
        mDatabaseReference.child(getUserId()).setValue(user);
    }


    @Override
    public void updateUICallback(int works) {
        //createUserData(uTime, uTime);

        switch (works) {
            case 1:
                updateUI(1);
                break;
            case 0:
                updateUI(0);
                break;
            case 2:
                updateUI(2);
                break;
            default:
                Log.v("MainActivity", "\"Callback check: Default\"");
                Toast.makeText(getApplicationContext(), "Callback check: Default", Toast.LENGTH_LONG).show();
        }
        Log.v("MainActivity", "Callback:" + works);
         Toast.makeText(getApplicationContext(), "Callback:" + works, Toast.LENGTH_LONG).show();

    }

    private void updateUI(int mFlag) {
        switch (mFlag) {
            case 1:
                mWifiStatuses.setText(getString(R.string.msg_connected));
                mWifiStatuses.setTextColor(mConnectedColor);
                mCheckedIn.setText(getString(R.string.yes));
                mCheckedIn.setTextColor(mConnectedColor);
                break;
            case 0:
                mWifiStatuses.setText(getString(R.string.msg_disconnected));
                mWifiStatuses.setTextColor(mDisconnectedColor);
                mCheckedIn.setText(getString(R.string.no));
                mCheckedIn.setTextColor(mDisconnectedColor);
                mCheckedOut.setText(getString(R.string.yes));
                mCheckedOut.setTextColor(mConnectedColor);
                break;
            case 2:
                mWifiStatuses.setText(getString(R.string.msg_wifi_closed));
                mWifiStatuses.setTextColor(mDisconnectedColor);
                mCheckedIn.setText(getString(R.string.no));
                mCheckedIn.setTextColor(mDisconnectedColor);
                mCheckedOut.setText(getString(R.string.yes));
                mCheckedOut.setTextColor(mConnectedColor);

                break;

            default:

        }
    }

    /*** User data change listener**/
//    private void dataChangeListener() {
//
//        mDatabaseReference.child(userId).addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//
//                UserData userData = dataSnapshot.getValue(UserData.class);
//                // Display newly updated name and button status
//
//
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//                Toast.makeText(getApplicationContext(), "Add Error", Toast.LENGTH_LONG).show();
//            }
//        });
//
//    }

}
