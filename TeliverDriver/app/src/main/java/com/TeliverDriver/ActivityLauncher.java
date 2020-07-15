package com.TeliverDriver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.teliver.sdk.core.TLog;
import com.teliver.sdk.core.Teliver;
import com.teliver.sdk.models.UserBuilder;

@SuppressWarnings("ALL")
public class ActivityLauncher extends AppCompatActivity implements View.OnClickListener, FragmentTrip.TripState,OnSuccessListener<Location> {

    private static final int GPS_REQ = 124;
    private boolean inCurrentTrip;

    private String driverName = "driver_4";


    private enum type {
        trip,
        push
    }

    private Application application;

    private TextView txtTrips, txtEventPush;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        Teliver.identifyUser(new UserBuilder(driverName).setUserType(UserBuilder.USER_TYPE.OPERATOR).registerPush().build());
        TLog.setVisible(true);
    }


    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        application = (Application) getApplication();
        txtTrips = (TextView) findViewById(R.id.txtTrips);
        txtEventPush = (TextView) findViewById(R.id.txtEventPush);
        txtTrips.setOnClickListener(this);
        txtEventPush.setOnClickListener(this);
        setFragment(0);
        if (Application.checkLPermission(this))
            enableGPS(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.txtTrips:
                setFragment(0);
                break;
            case R.id.txtEventPush:
                if (application.getBooleanInPef(Constants.IN_CURRENT_TRIP))
                    setFragment(1);
                else
                    Toast.makeText(this, "your trip is not active to send a Event Push", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void setFragment(int i) {
        android.app.Fragment fragment = null;
        switch (i) {
            case 0:
                FragmentTrip fragmentTrip = new FragmentTrip();
                fragmentTrip.setTripListener(this);
                fragment = fragmentTrip;
                break;
            case 1:
                fragment = new FragmentPush();
                break;
            default:
                fragment = new FragmentTrip();
                break;
        }
        android.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_view, fragment);
        transaction.commit();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 3 && resultCode == RESULT_OK)
            Toast.makeText(ActivityLauncher.this, "Gps is turned on", Toast.LENGTH_SHORT).show();
        else if (requestCode == 3 && resultCode == RESULT_CANCELED)
            finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==115){
            if (!Application.isPermissionOk(grantResults)){
                Toast.makeText(this,"Location permission denied",Toast.LENGTH_SHORT).show();
                finish();
            }
            else enableGPS(this);
        }
    }


    public void enableGPS(final
                          OnSuccessListener<Location> listener) {
        try {
            final FusedLocationProviderClient client = LocationServices
                    .getFusedLocationProviderClient(this);
            final LocationRequest locationRequest = getLocationReq();
            LocationSettingsRequest request = new LocationSettingsRequest
                    .Builder().addLocationRequest(locationRequest)
                    .setAlwaysShow(true).build();
            Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(
                    this).checkLocationSettings(request);
            task.addOnSuccessListener(locationSettingsResponse ->
                    getMyLocation(client, locationRequest, listener));
            task.addOnFailureListener(e -> {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(this, GPS_REQ);
                    } catch (IntentSender.SendIntentException sendEx) {
                        listener.onSuccess(null);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    private static void getMyLocation(final FusedLocationProviderClient client
            , LocationRequest locationRequest, final OnSuccessListener<Location> listener) {
        try {
            client.requestLocationUpdates(locationRequest, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null)
                        Log.d("OnDemandDelivery::","result null");
                    else {
                        Location location = locationResult.getLastLocation();
                        client.removeLocationUpdates(this);
                        listener.onSuccess(location);
                    }
                }
            }, Looper.myLooper());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static LocationRequest getLocationReq() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(2000);
        return locationRequest;
    }


    @Override
    public void onSuccess(Location location) {
        Log.e("onSuccess::",location.getLatitude()+"");

    }

    @Override
    public void tripStarted() {
        txtEventPush.setBackground(getResources().getDrawable(R.drawable.oval_background));
    }

    @Override
    public void tripStopped() {
        txtEventPush.setBackground(getResources().getDrawable(R.drawable.unselected_background));
    }


    @Override
    protected void onResume() {
        if (application.getBooleanInPef(Constants.IN_CURRENT_TRIP))
            txtEventPush.setBackground(getResources().getDrawable(R.drawable.oval_background));
        super.onResume();
    }
}
