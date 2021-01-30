package com.example.loggerappv02;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Regions;
import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    String TAG = "LoggerDebug";

    TerminalFragment terminal;

    // ---- AWS ----
    AWSIotMqttManager m;
    CognitoCachingCredentialsProvider credentialsProvider;
    String clientId;
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a3azmxc45machk-ats.iot.ap-southeast-2.amazonaws.com";

    // ---- Layout ----
    TextView awsStatus;
    TextView correcTV;
    TextView topicTV;
    TextView recvTV;
    Button unSubBtn;
    Button subBtn;
    // ---- Bluetooth ----
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> listItems = new ArrayList<>();
    private ArrayAdapter<BluetoothDevice> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ---- Bluetooth ----
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // ---- Layout objects ----
        awsStatus = (TextView) findViewById(R.id.aws_Status_tv);
        correcTV = (TextView) findViewById(R.id.receivTV);
        topicTV = (TextView) findViewById(R.id.sub_top_tv);
        recvTV = (TextView) findViewById(R.id.receivTV);
        unSubBtn = (Button) findViewById(R.id.unsub_btn);
        subBtn = (Button) findViewById(R.id.sub_btn);
        unSubBtn.setEnabled(false);
        // ---- AWS ----
        clientId = UUID.randomUUID().toString();
        try {
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
        );     m = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);
        m.setMaxAutoReconnectAttempts(2);

        // Connect to AWS through Amazon Cognito
        try {
            m.connect(credentialsProvider, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status, final Throwable throwable) {
                    Log.d(TAG, "Status = " + String.valueOf(status));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (status == AWSIotMqttClientStatus.Connecting) {
                                awsStatus.setText("Connecting to AWS");
                                Log.d(TAG, "Connecting");

                            } else if (status == AWSIotMqttClientStatus.Connected) {
                                awsStatus.setText("Connected to AWS");
                                Log.d(TAG, "Connected");

                            } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                if (throwable != null) {
                                    Log.e(TAG, "Connection error.", throwable);
                                }
                                awsStatus.setText("Reconnecting to AWS");
                            } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                if (throwable != null) {
                                    throwable.printStackTrace();
                                }
                                awsStatus.setText("Disconnected from AWS");
                                Log.d(TAG, "Disconnected");
                            } else {
                                awsStatus.setText("Disconnected from AWS");
                                Log.d(TAG, "Disconnected");
                            }
                        }
                    });
                }
            });
        } catch (final Exception e) {
            Log.e(TAG, "Connection error.", e);
            awsStatus.setText("Error! " + e.getMessage());
        }
    }

    public void Subscribe(View view) {

        final String topic = topicTV.getText().toString();
        topicTV.setEnabled(false);
        subBtn.setEnabled(false);
        unSubBtn.setEnabled(true);

        Log.d(TAG, "topic = " + topic);
        try {

            m.subscribeToTopic(topic, AWSIotMqttQos.QOS0, new AWSIotMqttNewMessageCallback() {
                @Override
                public void onMessageArrived(final String topic, final byte[] data) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String message = new String(data, "UTF-8");
                                Log.d(TAG, "Message arrived:");
                                Log.d(TAG, "   Topic: " + topic);
                                //Log.d(TAG, " Message: " + message);

                                recvTV.setText("Receiving messages" +'\n' + message.substring(0, 20) );

                            } catch (final Exception e) {
                                Log.e(TAG, "Message encoding error.", e);
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Subscription error.", e);
            recvTV.setText("Subscription error."+ e.toString());
        }
    }

    public void Unsubscribe(View view) {
        final String topic = topicTV.getText().toString();
        m.unsubscribeTopic(topic);
        topicTV.setEnabled(true);
        subBtn.setEnabled(true);
        unSubBtn.setEnabled(false);
    }
}