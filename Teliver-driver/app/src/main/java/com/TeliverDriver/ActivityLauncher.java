package com.TeliverDriver;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.teliver.sdk.core.TripListener;
import com.teliver.sdk.models.PushData;
import com.teliver.sdk.models.Trip;
import com.teliver.sdk.models.TripBuilder;
import com.teliver.sdk.models.UserBuilder;

@SuppressWarnings("ALL")
public class ActivityLauncher extends AppCompatActivity implements View.OnClickListener {

    private Button btnTrip, btnPush;

    private boolean inCurrentTrip;

    private String driverName = "driver_20";

    private enum type {
        trip,
        push
    }

    private Application application;

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
        btnTrip = (Button) findViewById(R.id.btnTrip);
        btnPush = (Button) findViewById(R.id.btnPush);
        btnTrip.setOnClickListener(this);
        btnPush.setOnClickListener(this);
        application = (Application) getApplication();
        if (Application.checkPermission(this))
            checkGps();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnTrip:
                if (!inCurrentTrip)
                    inflateDialog(type.trip);
                else {
                    stopTrip();
                    Toast.makeText(this, "Trip Stopped", Toast.LENGTH_SHORT).show();
                    btnTrip.setText(getString(R.string.txtStartTrip));
                    btnPush.setVisibility(View.GONE);
                    inCurrentTrip = !inCurrentTrip;
                    application.storeBooleanInPref("PUSH_BUTTON_VISIBLE", false);
                }
                break;
            case R.id.btnPush:
                inflateDialog(type.push);
                break;
            default:
                break;
        }
    }


    private void inflateDialog(type type) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.layout_trip, null);
        LinearLayout layoutTrip = (LinearLayout) view.findViewById(R.id.layoutTrip);
        LinearLayout layoutEventPush = (LinearLayout) view.findViewById(R.id.layoutEventPush);
        final EditText edtTrackingId = (EditText) view.findViewById(R.id.edtTrackingId);
        final EditText edtUserName = (EditText) view.findViewById(R.id.edtUserName);
        final EditText edtPushMessage = (EditText) view.findViewById(R.id.edtPushMessage);
        final Button btnStartTrip = (Button) view.findViewById(R.id.btnStartTrip);
        final Button btnEventPush = (Button) view.findViewById(R.id.btnEventPush);
        final TextInputLayout layoutTrackingId = (TextInputLayout) view.findViewById(R.id.layoutTrackingId);
        final TextInputLayout layoutUserName = (TextInputLayout) view.findViewById(R.id.layoutUserName);
        final TextInputLayout layoutPushMessage = (TextInputLayout) view.findViewById(R.id.layoutPushMessage);

        if (type == ActivityLauncher.type.trip)
            layoutEventPush.setVisibility(View.GONE);
        else if (type == ActivityLauncher.type.push)
            layoutTrip.setVisibility(View.GONE);

        alert.setView(view);
        final AlertDialog dialog = alert.create();
        dialog.show();

        btnStartTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutTrackingId.setErrorEnabled(false);
                layoutTrackingId.setError(null);
                String trackingId = edtTrackingId.getText().toString().trim();
                application.storeStringInPref(Application.TRACKING_ID, trackingId);
                if (trackingId.isEmpty())
                    layoutTrackingId.setError(getString(R.string.txtErrorTrackingId));
                else {
                    dialog.dismiss();
                    startTrip(application.getStringInPref(Application.TRACKING_ID));
                }
            }
        });

        btnEventPush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutUserName.setError(null);
                layoutUserName.setErrorEnabled(false);
                layoutPushMessage.setError(null);
                layoutPushMessage.setErrorEnabled(false);
                String username = edtUserName.getText().toString().trim();
                String pushMessage = edtPushMessage.getText().toString().trim();
                if (username.isEmpty())
                    layoutUserName.setError(getString(R.string.txtErrorUserName));
                else if (pushMessage.isEmpty())
                    layoutPushMessage.setError(getString(R.string.txtErrorMessageEmpty));
                else {
                    dialog.dismiss();
                    sendEventPush(username, pushMessage);
                    Toast.makeText(ActivityLauncher.this, "Message Sended", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendEventPush(String username, String pushMessage) {
        PushData pushData = new PushData(username);
        pushData.setMessage(pushMessage);
        pushData.setPayload(pushMessage);
        Teliver.sendEventPush(application.getStringInPref(Application.TRACKING_ID), pushData, "tag");
    }

    private void startTrip(String trackingId) {
        Teliver.startTrip(new TripBuilder(trackingId).build());
        Teliver.setListener(new TripListener() {
            @Override
            public void onTripStarted(Trip tripDetails) {
                Log.d("TELIVER::", "onTripStarted: ==  " + tripDetails.getTrackingId() + " trip id == " + tripDetails.getTripId());
                Toast.makeText(ActivityLauncher.this, "Trip Started", Toast.LENGTH_SHORT).show();
                btnTrip.setText(getString(R.string.txtStoptrip));
                inCurrentTrip = !inCurrentTrip;
                btnPush.setVisibility(View.VISIBLE);
                application.storeBooleanInPref("PUSH_BUTTON_VISIBLE", true);
            }

            @Override
            public void onTripEnded(String trackingID) {
                Log.d("TELIVER::", "onTripEnded: ==  " + trackingID);
            }

            @Override
            public void onTripError(String reason) {
                Log.d("TELIVER::", "onTripError: ==  " + reason);
            }
        });
    }

    private void stopTrip() {
        Teliver.stopTrip(application.getStringInPref(Application.TRACKING_ID));
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
    protected void onResume() {
        if (application.getBooleanInPef("PUSH_BUTTON_VISIBLE")) {
            btnPush.setVisibility(View.VISIBLE);
            btnTrip.setText(getString(R.string.txtStoptrip));
            inCurrentTrip = true;
        }
        super.onResume();

    }
}
