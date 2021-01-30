package com.example.loggerappv03;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link gpsReturn#newInstance} factory method to
 * create an instance of this fragment.
 */
public class gpsReturn extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    TextView output_tv;
    BluetoothDataViewModel bluetoothDataViewModel;


    public gpsReturn() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment gpsReturn.
     */
    // TODO: Rename and change types and number of parameters
    public static gpsReturn newInstance(String param1, String param2) {
        gpsReturn fragment = new gpsReturn();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

         */
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gps_return, container, false);
        output_tv = (TextView) view.findViewById(R.id.gps_return_data_tv);
        output_tv.setText("Startup");

        bluetoothDataViewModel = new ViewModelProvider(requireActivity()).get(BluetoothDataViewModel.class);
        bluetoothDataViewModel.getHpposllh().observe(getViewLifecycleOwner(), new Observer<byte []>() {
            @Override
            public void onChanged(byte[] bytes) {
                //output_tv.setText(String.format("%8s", Integer.toBinaryString(bytes[10] & 0xFF)).replace(' ', '0'));
                decode(bytes);
            }
        });
        return view;
    }

    public long getUInt32(byte [] bytes){
        long value = bytes[0] & 0xFF;
        value |= (bytes[1] << 8) & 0xFFFF;
        value |= (bytes[2] << 16) & 0xFFFFFF;
        value |= (bytes[3] << 24) & 0xFFFFFFFF;
        return value;
    }

    public void decode(byte[] data) {
        //decodes UBX-NAV-HPPOSLLH
        //byte[] data = bluetoothDataViewModel.getHpposllh().getValue();
        if (data.length <= 37) {
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
            //output_tv.append(String.valueOf(iTOW));
            output_tv.setText(String.valueOf(iTOW));

        } else {
            //output_tv.append("err" + String.valueOf(data.length));//TextUtil.toHexString(new byte[]{data[0], data[1], data[2], data[3], data[4], data[5], data[6],data[7]}));
        }
    }
}