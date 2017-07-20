package com.TeliverDriver;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.teliver.sdk.core.TLog;
import com.teliver.sdk.core.Teliver;
import com.teliver.sdk.models.UserBuilder;

@SuppressWarnings("ALL")
public class ActivityLauncher extends AppCompatActivity implements View.OnClickListener, FragmentTrip.TripState {

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
        if (Application.checkPermission(this))
            checkGps();
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


    private void checkGps() {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                Status status = locationSettingsResult.getStatus();
                if (status.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    try {
                        status.startResolutionForResult(ActivityLauncher.this, 3);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
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
        switch (requestCode) {
            case 4:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED && requestCode == 4)
                    finish();
                else checkGps();
                break;
        }
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
