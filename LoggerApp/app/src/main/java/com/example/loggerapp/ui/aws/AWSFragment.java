package com.example.loggerapp.ui.aws;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.loggerapp.R;

public class AWSFragment extends Fragment {

    private AWSViewModel AWSViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AWSViewModel =
                new ViewModelProvider(this).get(AWSViewModel.class);
        View root = inflater.inflate(R.layout.fragment_aws, container, false);
        final TextView textView = root.findViewById(R.id.text_dashboard);

        AWSViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        return root;
    }
}