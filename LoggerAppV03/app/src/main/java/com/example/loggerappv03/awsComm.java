package com.example.loggerappv03;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.Arrays;
import java.util.UUID;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;

public class awsComm extends Fragment {

    private AwsCommViewModel mViewModel;
    Button subBtn;
    Button unSubBtn;
    TextView status_tv;
    String TAG = "awsComm: ";
    String topic = "baseStation/173829353/-41505723";
    TextView topicMsg_tv;
    private View view;

    // ---- AWS ----
    AWSIotMqttManager m;
    CognitoCachingCredentialsProvider credentialsProvider;
    String clientId;
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a3azmxc45machk-ats.iot.ap-southeast-2.amazonaws.com";

    public static awsComm newInstance() {
        return new awsComm();
    }

    private View.OnClickListener SubOnClickListener = v -> AwsSub(topic);
    private View.OnClickListener UnSubOnClickListener = v -> AwsUnSub(topic);
    BluetoothDataViewModel bluetoothDataViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.aws_comm_fragment, container, false);

        status_tv = (TextView) view.findViewById(R.id.aws_state_tv);
        subBtn = (Button) view.findViewById(R.id.aws_sub_btn);
        unSubBtn = (Button) view.findViewById(R.id.aws_unSub_btn);
        subBtn.setOnClickListener(SubOnClickListener);
        unSubBtn.setOnClickListener(UnSubOnClickListener);
        topicMsg_tv = (TextView) view.findViewById(R.id.aws_topic_msg_tv);

        // ---- AWS ID and connection ----
        clientId = UUID.randomUUID().toString();
        try {
            Amplify.addPlugin(new AWSCognitoAuthPlugin());
            Amplify.configure(getActivity().getApplicationContext());
            Log.i("MyAmplifyApp", "Initialized Amplify");
        } catch (AmplifyException error) {
            Log.e("MyAmplifyApp", "Could not initialize Amplify", error);
        }

        // Initialize the Amazon Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getActivity().getApplicationContext(),
                "ap-southeast-2:c8b6ec77-5e40-4bb8-82b0-3bd077fe3205", // Identity pool ID
                Regions.AP_SOUTHEAST_2 // Region
        );
        m = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);
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
                                status_tv.setText("Connecting to AWS");
                                Log.d(TAG, "Connecting");

                            } else if (status == AWSIotMqttClientStatus.Connected) {
                                status_tv.setText("Connected to AWS");
                                Log.d(TAG, "Connected");

                            } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                if (throwable != null) {
                                    Log.e(TAG, "Connection error.", throwable);
                                }
                                status_tv.setText("Reconnecting to AWS");
                            } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                if (throwable != null) {
                                    throwable.printStackTrace();
                                }
                                status_tv.setText("Disconnected from AWS");
                                Log.d(TAG, "Disconnected");
                            } else {
                                status_tv.setText("Disconnected from AWS");
                                Log.d(TAG, "Disconnected");
                            }
                        }
                    });
                }
            });
        } catch (final Exception e) {
            Log.e(TAG, "Connection error.", e);
            status_tv.setText("Error! " + e.getMessage());
        }

        bluetoothDataViewModel = new ViewModelProvider(requireActivity()).get(BluetoothDataViewModel.class);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(AwsCommViewModel.class);
        // TODO: Use the ViewModel
    }

    private void AwsSub(String topic) {

        subBtn.setEnabled(false);
        unSubBtn.setEnabled(true);

        Log.d(TAG, "topic = " + topic);
        try {

            m.subscribeToTopic(topic, AWSIotMqttQos.QOS0, new AWSIotMqttNewMessageCallback() {
                @Override
                public void onMessageArrived(final String topic, byte[] data) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                //String message = new String(data, "UTF-8");
                                //Log.d(TAG, "Message arrived:");
                                //Log.d(TAG, "   Topic: " + topic);
                                //Log.d(TAG, " Message: " + message);
                                //String shortMsg = message.substring(0, 20);
                                //topicMsg_tv.setText(TextUtil.toHexString(data).substring(0, 20) + " length =" + String.valueOf(data.length));

                            } catch (final Exception e) {
                                Log.e(TAG, "Message encoding error.", e);
                            }

                            bluetoothDataViewModel.setRTCM3(data);
                        }
                    });

                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Subscription error.", e);
        }
        // Error state
        return;
    }



    public byte[] store = {0};
    public byte[] orderRtcm(byte[] data) {
        byte[] newStore = new byte[store.length + data.length];
        System.arraycopy(store, 0, newStore, 0, store.length);
        System.arraycopy(data, 0, newStore, store.length, data.length);
        store = newStore;
        int firstIndex = 99999;
        int secIndex = 99999;
        byte[] tempArray = new byte[10000];
        int tempArrayIndex = 0;
        int offset = 1;
        for (int i = 0; i < store.length - 1; i++) {
            if ((int)store[i] == ((int)(byte) 0xd3)) {
                if (firstIndex == 99999) {
                    firstIndex = i;
                } else {
                    secIndex = i;
                    System.arraycopy(store, firstIndex, tempArray, tempArrayIndex, (secIndex - firstIndex - 1));
                    tempArrayIndex += (secIndex - firstIndex-1) + offset;
                    offset =0;
                    firstIndex = secIndex;
                }
            }
        }
        return tempArray;
    }



    public void AwsUnSub(String topic) {
        m.unsubscribeTopic(topic);
        subBtn.setEnabled(true);
        unSubBtn.setEnabled(false);
    }
}