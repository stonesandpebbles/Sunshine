package com.example.android.sunshine.app;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by Abhijeet on 06-Oct-16.
 */
public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {
    private final String TAG = MyFirebaseInstanceIDService.class.getSimpleName();
    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(String token) {
        Log.i(TAG, "sendRegistrationToServer: Token : " + token);
    }
}
