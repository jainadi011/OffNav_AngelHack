package com.zriton.offnav;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.zriton.offnav.adapter.DirectionAdapter;
import com.zriton.offnav.model.ModelDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by aditya on 16/7/16.
 */
public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    DirectionAdapter directionAdapter;
    ArrayList<ModelDirection> modelDirectionArrayList = new ArrayList<>();
    LinkedHashSet hs = new LinkedHashSet();
    Button gps,sendSMS;
    EditText source,destination;
    static MainActivity instance;
    Location location;
    RelativeLayout errorLayout;
    TextView reason;
    ProgressDialog progressDialog;

    public static void smsReceived()
    {
        //instance.fetchSMS();
    }

    static class TimerResult extends TimerTask {

        @Override
        public void run() {
            instance.fetchSMS();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        instance = MainActivity.this;
        final LocationManager locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(locationManager == null || getLastBestLocation(locationManager) ==null )
                {
                    promptEnableGps();
                }
                else
                {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    } else {
                        location = getLastBestLocation(locationManager);
                        source.setText( "Lat: " + location.getLatitude() + " Lng: "
                                + location.getLongitude());
                        Toast.makeText(
                                getBaseContext(),
                                "Lat: " + location.getLatitude() + " Lng: "
                                        + location.getLongitude(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        sendSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (source.getText().toString().equals("")) {
                    Toast.makeText(MainActivity.this, "Please enter approximate source or use GPS", Toast.LENGTH_SHORT).show();
                } else if (destination.getText().toString().equals("")) {
                    Toast.makeText(MainActivity.this, "Please enter approximate destination", Toast.LENGTH_SHORT).show();
                } else {
                    Timer t = new Timer();
                    t.schedule(new TimerResult(), 30000);
                    showProgressBar();
                    if (source.getText().toString().contains("Lat:"))
                        sendSMS("+12513331811", location.getLatitude() + "," + location.getLongitude() + "%" + destination.getText());
                    else
                        sendSMS("+12513331811", source.getText() + "%" + destination.getText());
                }
            }
        });

    }

    private void showProgressBar()
    {
        progressDialog.setMessage("Waiting for directions to arrive...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideDialog()
    {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
    }
    //Sends an SMS message to another device

    private void sendSMS(String phoneNumber, String message) {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);
    }

    private Location getLastBestLocation(LocationManager mLocationManager) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        else {
            Location locationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location locationNet = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            long GPSLocationTime = 0;
            if (null != locationGPS) {
                GPSLocationTime = locationGPS.getTime();
            }

            long NetLocationTime = 0;

            if (null != locationNet) {
                NetLocationTime = locationNet.getTime();
            }

            if (0 < GPSLocationTime - NetLocationTime) {
                return locationGPS;
            } else {
                return locationNet;
            }
        }
        return null;
    }

    private void initView() {
        progressDialog = new ProgressDialog(MainActivity.this);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        gps = (Button)findViewById(R.id.gps);
        source = (EditText)findViewById(R.id.source);
        destination = (EditText)findViewById(R.id.destination);
        sendSMS= (Button)findViewById(R.id.sendSMS);
        directionAdapter = new DirectionAdapter(MainActivity.this, modelDirectionArrayList);
        recyclerView.setAdapter(directionAdapter);

        errorLayout = (RelativeLayout)findViewById(R.id.errorLayout);
        reason = (TextView)findViewById(R.id.reason);
    }

    //Fetch SMS from received twilio account
    private void fetchSMS()
    {
        Uri message = Uri.parse("content://sms/");
        ContentResolver cr  = getContentResolver();

        Cursor c = cr.query(message, null, null, null, null);
        modelDirectionArrayList.clear();
        startManagingCursor(c);
        if(c!=null)
        {
            int totalSMS = c.getCount();

            // Read the sms data and store it in the list

            if (c.moveToFirst()) {

                for (int i = 0; i < totalSMS; i++) {
                    if(c.getString(c.getColumnIndexOrThrow("body")).toString().contains("Sent from your Twilio"))
                    {
                        String temp = c.getString(c.getColumnIndexOrThrow("body")).toString();
                        temp = temp.substring(38,temp.length());
                                System.out.print(temp);
                        ModelDirection modelDirection = new ModelDirection();
                        if(temp.substring(0,1).equals("-"))
                        modelDirection.flag = Integer.parseInt(temp.substring(0,2));
                        else
                            modelDirection.flag = Integer.parseInt(temp.substring(0,1));
                        if(modelDirection.flag == -1)
                        {
                            recyclerView.setVisibility(View.GONE);
                            errorLayout.setVisibility(View.VISIBLE);
                            reason.setVisibility(View.VISIBLE);
                            reason.setText("Current SMS service is not able to provide directions for locations this far. Please try to narrow your search");
                            break;
                        }
                        else if(modelDirection.flag == -2)
                        {
                            recyclerView.setVisibility(View.GONE);
                            errorLayout.setVisibility(View.VISIBLE);
                            reason.setVisibility(View.VISIBLE);
                            reason.setText("Source is incorrect");
                            break;
                        }
                        else if(modelDirection.flag == -3)
                        {
                            recyclerView.setVisibility(View.GONE);
                            errorLayout.setVisibility(View.VISIBLE);
                            reason.setVisibility(View.VISIBLE);
                            reason.setText("Destination is incorrect");
                            break;
                        }
                        if(modelDirection.flag == -4)
                        {
                            recyclerView.setVisibility(View.GONE);
                            errorLayout.setVisibility(View.VISIBLE);
                            reason.setVisibility(View.VISIBLE);
                            reason.setText("No possible routes found");
                            break;
                        }
                        else {
                            recyclerView.setVisibility(View.VISIBLE);
                            errorLayout.setVisibility(View.GONE);
                            reason.setVisibility(View.GONE);
                            String content[] = temp.substring(2, temp.length()).split("%");
                            content[0] = content[0].replace("@H", "Head");
                            content[0] = content[0].replace("@N", "North");
                            content[0] = content[0].replace("@E", "East");
                            content[0] = content[0].replace("@W", "West");
                            content[0] = content[0].replace("@S", "South");
                            content[0] = content[0].replace("@2", "towards");
                            content[0] = content[0].replace("@T", "Turn");
                            content[0] = content[0].replace("@L", "Left");
                            content[0] = content[0].replace("@R", "Right");
                            content[0] = content[0].replace("@SR", "Slight Right");
                            content[0] = content[0].replace("@O", "Onto");
                            modelDirection.content = content[0];
                            if (content.length > 1)
                                modelDirection.distance = content[1];
                            else
                                modelDirection.distance = "0";
                            modelDirectionArrayList.add(modelDirection);
                        }
                    }

                    c.moveToNext();
                }
                Collections.sort(modelDirectionArrayList, new Comparator<ModelDirection>() {

                    public int compare(ModelDirection o1, ModelDirection o2) {
                        return o1.flag - o2.flag;
                    }
                });
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        directionAdapter = new DirectionAdapter(MainActivity.this, modelDirectionArrayList);
                        recyclerView.setAdapter(directionAdapter);

                    }
                });

            }
            c.close();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                hideDialog();

            }
        });

    }





    /**
     * Ask the user if they want to enable GPS
     */
    private void promptEnableGps() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.enable_gps_message))
                .setPositiveButton(getString(R.string.enable_gps_positive_button),
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(
                                        Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(intent);
                            }
                        }
                )
                .setNegativeButton(getString(R.string.enable_gps_negative_button),
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                )
                .show();
    }

}
