package com.example.loggerappv03;

import android.widget.LinearLayout;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.lang.reflect.Array;
import java.util.Arrays;

public class BluetoothDataViewModel extends ViewModel {
    private final MutableLiveData<byte []> hpposllh = new MutableLiveData<byte []>();
    private final MutableLiveData<byte []> RTCM3 = new MutableLiveData<byte []>();
    byte[] store = {(byte)0x00};

    public LiveData<byte []> getHpposllh(){
        return hpposllh;
    }

    public LiveData<byte[]> getRTCM3(){
        return RTCM3;
    }

    public void setHpposllh(byte [] data){
        hpposllh.setValue(data);
    }

    public void setRTCM3(byte [] data){
        RTCM3.setValue(data);
    }

}
