package com.TeliverDriver;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.teliver.sdk.core.Teliver;
import com.teliver.sdk.models.PushData;


public class FragmentPush extends Fragment {

    private EditText edtUserName, edtPushMessage;

    private TextInputLayout layoutUsername, layoutPushMessage;

    private Button btnSendPush;

    private Application application;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_push, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        application = (Application) getActivity().getApplicationContext();
        edtUserName = (EditText) view.findViewById(R.id.edtUserName);
        edtPushMessage = (EditText) view.findViewById(R.id.edtPushMessage);
        layoutUsername = (TextInputLayout) view.findViewById(R.id.layoutUserName);
        layoutPushMessage = (TextInputLayout) view.findViewById(R.id.layoutPushMessage);
        btnSendPush = (Button) view.findViewById(R.id.btnEventPush);

        btnSendPush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableError(layoutUsername, layoutPushMessage);
                String pushMessage = edtPushMessage.getText().toString().trim();

                if (pushMessage.isEmpty())
                    layoutPushMessage.setError(getString(R.string.txtErrorMessageEmpty));
                else {
                    Log.d("TELIVER::", "onClick: " + " Event Push Sended");
                    String username = application.getStringInPref("userName");
                    String[] multipleId = username.split(",");
                    PushData pushData = new PushData(multipleId);
                    pushData.setMessage(pushMessage);
                    Teliver.sendEventPush(application.getStringInPref(Constants.TRACKING_ID), pushData, "tag");
                    edtUserName.setText("");
                    edtPushMessage.setText("");
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
}
