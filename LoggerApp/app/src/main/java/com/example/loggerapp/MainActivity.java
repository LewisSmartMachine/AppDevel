package com.example.loggerapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.regions.Regions;
import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.example.loggerapp.ui.aws.AWSFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amplifyframework.core.Amplify;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // ---- AWS ----
    AWSIotMqttManager m;
    CognitoCachingCredentialsProvider credentialsProvider;
    String clientId;
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a3azmxc45machk-ats.iot.ap-southeast-2.amazonaws.com";

    //-- AWS layout


    String TAG = "LoggerDebug";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //---- Layout ----
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_AWS, R.id.navigation_Bluetooth)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);


        // objects
        TextView statusView = (TextView) findViewById(R.id.ConnStat_TV);
        statusView.setText("Get out'a here");

        // ---- AWS ----
        clientId = UUID.randomUUID().toString();

        try {
            // Add this line, to include the Auth plugin.
            Amplify.addPlugin(new AWSCognitoAuthPlugin());
            Amplify.configure(getApplicationContext());
            Log.i("MyAmplifyApp", "Initialized Amplify");
        } catch (AmplifyException error) {
            Log.e("MyAmplifyApp", "Could not initialize Amplify", error);
        }

        // Initialize the Amazon Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "ap-southeast-2:c8b6ec77-5e40-4bb8-82b0-3bd077fe3205", // Identity pool ID
                Regions.AP_SOUTHEAST_2 // Region
        );
        m = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);


    }


    private void AwsDisConn() {
        try {
            m.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "Disconnect error.", e);
        }
    }

    public void AwsConn(View view) {
        try {
            m.setMaxAutoReconnectAttempts(2);
            m.connect(credentialsProvider, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status, final Throwable throwable) {
                    Log.d(TAG, "Status = " + String.valueOf(status));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (status == AWSIotMqttClientStatus.Connecting) {
                                //AWSFragment.setTV("Connecting...");
                                Log.d(TAG, "Connecting");

                            } else if (status == AWSIotMqttClientStatus.Connected) {
                                //AWSFragment.setTV("Connected");
                                Log.d(TAG, "Connected");

                            } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                if (throwable != null) {
                                    Log.e(TAG, "Connection error.", throwable);
                                }
                                //AWSFragment.setTV("Reconnecting");
                            } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                if (throwable != null) {
                                    throwable.printStackTrace();
                                }
                                //AWSFragment.setTV("Disconnected");
                                Log.d(TAG, "Disconnected");
                            } else {
                                //AWSFragment.setTV("Disconnected");
                                Log.d(TAG, "Disconnected");
                            }
                        }
                    });
                }
            });
        } catch (final Exception e) {
            Log.e(TAG, "Connection error.", e);
            //AWSFragment.setTV("Error! " + e.getMessage());
        }
    }
}