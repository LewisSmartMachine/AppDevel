package com.example.loggerappv04;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    Button awsConnBtn;
    TextView debugTv;
    private static final String TAG = "MainActivity";

    public View.OnClickListener ConnectToAwsLis = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            AwsConnect();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        debugTv = (TextView) findViewById(R.id.debug_tv);

        awsConnBtn = (Button) findViewById(R.id.aws_conn_btn);
        awsConnBtn.setOnClickListener(ConnectToAwsLis);
    }

    public void AwsConnect(){
        Log.d(TAG, "AwsConnect: Attempt");
        ExampleThread thread = new ExampleThread(10);
        thread.run();

    }
    class ExampleThread extends Thread {
        int seconds;
        ExampleThread(int seconds) {
            this.seconds = seconds;
        }
        @Override
        public void run() {
            for (int i = 0; i < seconds; i++) {
                Log.d(TAG, "startThread: " + i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

