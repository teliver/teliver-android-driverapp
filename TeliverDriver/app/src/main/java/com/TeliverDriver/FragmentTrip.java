package com.TeliverDriver;

import android.app.Fragment;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.teliver.sdk.core.Teliver;
import com.teliver.sdk.core.TripListener;
import com.teliver.sdk.models.PushData;
import com.teliver.sdk.models.Trip;
import com.teliver.sdk.models.TripBuilder;


public class FragmentTrip extends Fragment {

    private EditText edtUserName, edtPushMessage, edtTrackingId;

    private TextInputLayout layoutUsername, layoutPushMessage, layoutTrackingId;

    private Button btnStartTrip;

    private Application application;

    private TripState tripState;


    public interface TripState {
        void tripStarted();

        void tripStopped();
    }

    public void setTripListener(TripState tripState) {
        this.tripState = tripState;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trip, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        application = (Application) getActivity().getApplicationContext();
        edtTrackingId = view.findViewById(R.id.edtTrackingId);
        edtUserName = view.findViewById(R.id.edtUserName);
        edtPushMessage = view.findViewById(R.id.edtPushMessage);
        layoutTrackingId = view.findViewById(R.id.layoutTrackingId);
        layoutUsername = view.findViewById(R.id.layoutUserName);
        layoutPushMessage = view.findViewById(R.id.layoutPushMessage);
        btnStartTrip = view.findViewById(R.id.btnStartTrip);

        btnStartTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (application.getBooleanInPef(Constants.IN_CURRENT_TRIP)) {
                    String username = application.getStringInPref("userName");
                    String[] multipleUsers = username.split(",");
                    PushData pushData = new PushData(multipleUsers);
                    pushData.setMessage("trip_stopped");
                    Teliver.sendEventPush(application.getStringInPref(Constants.TRACKING_ID), pushData, "trip stopped");
                    Teliver.stopTrip(application.getStringInPref(Constants.TRACKING_ID));
                    Teliver.setTripListener(new TripListener() {
                        @Override
                        public void onTripStarted(Trip tripDetails) {

                        }

                        @Override
                        public void onLocationUpdate(Location location) {
                        }

                        @Override
                        public void onTripEnded(String trackingID) {
                            btnStartTrip.setText(getString(R.string.txtStartTrip));
                            edtTrackingId.setText("");
                            application.storeBooleanInPref(Constants.IN_CURRENT_TRIP, false);
                            application.deletePreference();
                            if (tripState != null)
                                tripState.tripStopped();
                            layoutUsername.setVisibility(View.VISIBLE);
                            layoutPushMessage.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onTripError(String reason) {

                        }
                    });
                } else {
                    disableError(layoutUsername, layoutTrackingId, layoutPushMessage);
                    String trackingId = edtTrackingId.getText().toString().trim();
                    application.storeStringInPref(Constants.TRACKING_ID, trackingId);
                    String username = edtUserName.getText().toString().trim();
                    application.storeStringInPref("userName", username);
                    String[] multipleId = username.split(",");
                    String pushMessage = edtPushMessage.getText().toString().trim();

                    if (trackingId.isEmpty())
                        layoutTrackingId.setError(getString(R.string.txtErrorTrackingId));
                    else if (username.isEmpty())
                        layoutUsername.setError(getString(R.string.txtErrorUserName));
                    else if (pushMessage.isEmpty())
                        layoutPushMessage.setError(getString(R.string.txtErrorMessageEmpty));
                    else {
                        PushData pushData = new PushData(multipleId);
                        pushData.setMessage(pushMessage);
                        TripBuilder tripBuilder = new TripBuilder(application.getStringInPref(Constants.TRACKING_ID)).
                                withUserPushObject(pushData);
                        Teliver.startTrip(tripBuilder.build());
                        Teliver.setTripListener(new TripListener() {
                            @Override
                            public void onTripStarted(Trip tripDetails) {
                                application.storeBooleanInPref(Constants.IN_CURRENT_TRIP, true);
                                btnStartTrip.setText(getString(R.string.txtStoptrip));
                                edtPushMessage.setText(" ");
                                edtUserName.setText(" ");
                                if (tripState != null)
                                    tripState.tripStarted();
                                layoutUsername.setVisibility(View.GONE);
                                layoutPushMessage.setVisibility(View.GONE);
                            }

                            @Override
                            public void onLocationUpdate(Location location) {

                            }

                            @Override
                            public void onTripEnded(String trackingID) {

                            }

                            @Override
                            public void onTripError(String reason) {

                            }
                        });
                    }

                }
            }
        });
    }


    private void disableError(TextInputLayout... layouts) {
        for (TextInputLayout textInputLayout : layouts) {
            textInputLayout.setError(null);
            textInputLayout.setErrorEnabled(false);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        if (application.getBooleanInPef(Constants.IN_CURRENT_TRIP)) {
            btnStartTrip.setText(getString(R.string.txtStoptrip));
            edtTrackingId.setText(application.getStringInPref(Constants.TRACKING_ID));
            layoutUsername.setVisibility(View.GONE);
            layoutPushMessage.setVisibility(View.GONE);
        }
        super.onResume();
    }
}
