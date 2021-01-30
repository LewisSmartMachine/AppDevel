package com.example.loggerappv03;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AwsCommViewModel extends ViewModel {
    private final MutableLiveData<String> testString = new MutableLiveData<String>();

    public LiveData<String> getHpposllh(){
        return testString;
    }

    public void setHpposllh(String data){
        testString.setValue(data);
    }
}