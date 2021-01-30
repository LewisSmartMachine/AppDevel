package com.example.loggerappv03;

import android.app.Activity;
import android.graphics.BlendMode;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.LongDef;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import java.util.Vector;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.regions.Regions;
import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;

import java.util.Arrays;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {
    private int dataInt;
    private BluetoothDataViewModel bluetoothDataViewModel;
    private RelativeLayout myGpsLayout;
    private RelativeLayout myAwsLayout;
    private String TAG = "MainActivity: ";




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        if (savedInstanceState == null){
            //getSupportFragmentManager().beginTransaction().add(R.id.gps_data_layout, gpsReturn.class, null).commit();
            getSupportFragmentManager().beginTransaction().add(R.id.aws_msg_layout, awsComm.class, null).commit();
            getSupportFragmentManager().beginTransaction().add(R.id.fragment, new DevicesFragment(), "devices").commit();
        }else
            onBackStackChanged();


        // ---- custom UI ----
        iTOW_tv = (TextView) findViewById(R.id.iTOW_tv);
        myAwsLayout = (RelativeLayout) findViewById(R.id.aws_msg_layout);
        myGpsLayout = (RelativeLayout) findViewById(R.id.gps_data_layout);
        bluetoothDataViewModel = new ViewModelProvider(this).get(BluetoothDataViewModel.class);
        final boolean[] bt_conn = {false};
        bluetoothDataViewModel.getHpposllh().observe(this, new Observer<byte []>() {
            @Override
            public void onChanged(byte[] bytes) {
                //iTOW_tv.setText("Change" + '\n');
                if (!bt_conn[0]){
                    myAwsLayout.setVisibility(View.VISIBLE);
                    myGpsLayout.setVisibility(View.VISIBLE);
                    bt_conn[0] = true;
                }
            }
        });
        bluetoothDataViewModel.getRTCM3().observe(this, new Observer<byte[]>() {
            @Override
            public void onChanged(byte[] bytes) {
                //decodeRtcm3(bytes);
            }
        });


    }

    @Override
    public void onBackStackChanged() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount()>0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public int getdata(){
        return this.dataInt;
    }

    public void setdata(int dataInt){
        this.dataInt = dataInt;
    }

    public void Hello(View view) {
        Log.d("Hi",String.valueOf(dataInt));
    }

    public long getUInt32(byte [] bytes){
        long value = bytes[0] & 0xFF;
        value |= (bytes[1] << 8) & 0xFFFF;
        value |= (bytes[2] << 16) & 0xFFFFFF;
        value |= (bytes[3] << 24) & 0xFFFFFFFF;
        return value;
    }

    public void startTerminal(Bundle args){

    }

    TextView iTOW_tv;
    public void decodeHpposllh(byte[] data) {
        //decodes UBX-NAV-HPPOSLLH
        //byte[] data = bluetoothDataViewModel.getHpposllh().getValue();
        if (data.length <=  37) {
            String flags = String.format("%8s", Integer.toBinaryString(data[3] & 0xFF)).replace(' ', '0');
            long iTOW = getUInt32(new byte[]{data[6], data[(6 + 1)], data[(6 + 2)], data[(6 + 3)]});
            int lon = ((0xFF & data[10 + 3]) << 24) | ((0xFF & data[10 + 2]) << 16) |
                    ((0xFF & data[10 + 1]) << 8) | (0xFF & data[10]);
            int lat = ((0xFF & data[14 + 3]) << 24) | ((0xFF & data[14 + 2]) << 16) |
                    ((0xFF & data[14 + 1]) << 8) | (0xFF & data[14]);
            int height = ((0xFF & data[18 + 3]) << 24) | ((0xFF & data[18 + 2]) << 16) |
                    ((0xFF & data[18 + 1]) << 8) | (0xFF & data[18]);
            int hMSL = ((0xFF & data[22 + 3]) << 24) | ((0xFF & data[22 + 2]) << 16) |
                    ((0xFF & data[22 + 1]) << 8) | (0xFF & data[22]);
            short lonHp = (short) data[26];
            short latHp = (short) data[27];
            short heightHp = (short) data[28];
            short hMSLHp = (short) data[29];
            long hAcc = getUInt32(new byte[]{data[30], data[(30 + 1)], data[(30 + 2)], data[(30 + 3)]});
            // Not sure why I've run out of bytes? Ublox also gets weird readings for vAcc/hAcc
            //long vAcc = getUInt32(new byte[]{data[34], data[(34 + 1)], data[(34 + 2)], data[(34 + 3)]});
            iTOW_tv.append(String.valueOf(iTOW));

        }
        else{
            iTOW_tv.append("err"+ String.valueOf(data.length));//TextUtil.toHexString(new byte[]{data[0], data[1], data[2], data[3], data[4], data[5], data[6],data[7]}));
        }





    }

    public void decodeRtcm3(byte [] data){
        byte[] rtcmId = {(byte)0x01,(byte)0x02,(byte)0x03,(byte)0x04,(byte)0x05,(byte)0x06,(byte)0x07,(byte)0x09,(byte)0x0a,(byte)0xa1,(byte)0xa2,(byte)0x21,(byte)0x4a,(byte)0x4b,(byte)0x4d,(byte)0x54,(byte)0x55,(byte)0x57,(byte)0x5e,(byte)0x5f,(byte)0x61,(byte)0x7c,(byte)0x7d,(byte)0x7f,(byte)0xe6,(byte)0xfe,(byte)0xfd};
        iTOW_tv.setText("next," + '\n');
        for(int i = 0; i < data.length - 2; i++) {
            if (Arrays.equals(new byte[]{data[i]}, new byte[]{(byte) 0xd3})) {
                //for (int j =0; j< rtcmId.length; j++){
                    //if(Arrays.equals(new byte[]{data[i+1]}, new byte[]{rtcmId[j]})){
                        iTOW_tv.append(TextUtil.toHexString(new byte[] {data[i], data[i+1],data[i+2],data[i+3],data[i+4]}) + '\n');
                    //}
                //}
            }
        }
    }
}
