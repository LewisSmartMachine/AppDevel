package com.example.loggerappv03;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Random;

import static com.example.loggerappv03.TextUtil.newline_crlf;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected { False, Pending, True }

    private String deviceAddress;
    private SerialService service;

    private TextView receiveText;
    private TextView sendText;
    private TextUtil.HexWatcher hexWatcher;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean pendingNewline = false;
    private String newline = newline_crlf;

    BluetoothDataViewModel bluetoothDataViewModel;

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");
        // ---- my stuff ----
        bluetoothDataViewModel = new ViewModelProvider(requireActivity()).get(BluetoothDataViewModel.class);
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if(service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if(initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());

        sendText = view.findViewById(R.id.send_text);
        hexWatcher = new TextUtil.HexWatcher(sendText);
        hexWatcher.enable(hexEnabled);
        sendText.addTextChangedListener(hexWatcher);
        sendText.setHint(hexEnabled ? "HEX mode" : "");

        View sendBtn = view.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(v -> send(sendText.getText().toString()));

        Random rand = new Random();

        bluetoothDataViewModel.getRTCM3().observe(getViewLifecycleOwner(),new Observer<byte []>() {
            @Override
            public void onChanged(byte[] bytes) {
                try {
                    service.write(bytes);
                    receiveText.setText("Sent myId" + String.valueOf(rand.nextDouble()));
                } catch (IOException e) {
                    e.printStackTrace();
                    receiveText.setText("Not Sent");
                }

            }
        });
        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
        menu.findItem(R.id.hex).setChecked(hexEnabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            receiveText.setText("");
            return true;
        } else if (id == R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else if (id == R.id.hex) {
            hexEnabled = !hexEnabled;
            sendText.setText("");
            hexWatcher.enable(hexEnabled);
            sendText.setHint(hexEnabled ? "HEX mode" : "");
            item.setChecked(hexEnabled);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial + UI
     */
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }

    private void send(String str) {
        if(connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
            if(hexEnabled) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
            } else {
                msg = str;
                data = (str + newline).getBytes();
            }
            SpannableStringBuilder spn = new SpannableStringBuilder(msg+'\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    public long getUInt32(byte [] bytes){
        long value = bytes[0] & 0xFF;
        value |= (bytes[1] << 8) & 0xFFFF;
        value |= (bytes[2] << 16) & 0xFFFFFF;
        value |= (bytes[3] << 24) & 0xFFFFFFFF;
        return value;
    }


    private void receive(byte[] data) {
        if (data.length < 10) {
            //receiveText.append("to small"+'\n');
            return;
        }
        byte [] header0xB562 = {(byte) 0xB5, (byte)0x62};
        byte class0x01 = (byte) 0x01;
        byte id_HPPOSLLH = (byte) 0x14;
        byte id_Status  = (byte) 0x03;

        //((MainActivity)getActivity()).decode(data);
        if(hexEnabled) {
            //receiveText.append(String.valueOf(lon)+ "--"+ String.valueOf(lat) + "test " + String.valueOf(iTOW) +"Full Stream =" + "[" +TextUtil.toHexString(data) + "]"+'\n' + '\n');
//            receiveText.append("Header = ["+TextUtil.toHexString(Header) + "] iTOW = [" + String.valueOf(iTOW) + "] Lat = [" + String.valueOf(lat)+ "] lon = [" + String.valueOf(lon) + "] Full Stream =" + '\n' + "[" +TextUtil.toHexString(data) + "]"+'\n' + '\n');
            receiveText.append(TextUtil.toHexString(data) + '\n');
        } else {
/*
            String msg = new String(data);
            if(newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                // don't show CR as ^M if directly before LF
                msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
                // special handling if CR and LF come in separate fragments
                if (pendingNewline && msg.charAt(0) == '\n') {
                    Editable edt = receiveText.getEditableText();
                    if (edt != null && edt.length() > 1)
                        edt.replace(edt.length() - 2, edt.length(), "");
                }
                pendingNewline = msg.charAt(msg.length() - 1) == '\r';
            }
            receiveText.append(TextUtil.toCaretString(msg, newline.length() != 0));

 */
            for(int i = 0; i < data.length - 2; i++) {
                if (Arrays.equals(new byte[]{data[i], data[i + 1]}, header0xB562)) {

                    //receiveText.append('\n' + TextUtil.toHexString(new byte[]{data[i], data[i+1], data[i+2], data[i+3], data[i+4], data[i+5]}));
                    if ((data[i + 2] == class0x01)) {
                        //receiveText.append("one=["+String.format("%8s", Integer.toBinaryString(data[3] & 0xFF)).replace(' ', '0')+'\n'+"two=["+String.format("%8s", Integer.toBinaryString(id_HPPOSLLH & 0xFF)).replace(' ', '0'));

                        if (Arrays.equals(new byte[]{(byte) data[3]}, new byte[]{(byte) id_HPPOSLLH})) {//new byte[]{data[3]}, new byte[]{id_HPPOSLLH})) {
                            byte[] hpposPayload = Arrays.copyOfRange(data, 4, 41);
                            bluetoothDataViewModel.setHpposllh(hpposPayload);

                            //receiveText.append('\n' + TextUtil.toHexString(new byte[]{data[0], data[1], data[2], data[3], data[4], data[5]}) + " == " + TextUtil.toHexString(new byte[]{header0xB562[0], header0xB562[1], class0x01, id_HPPOSLLH}));
                            //((MainActivity) getActivity()).decode();//hpposPayload);
                            //receiveText.append("Found hp" + '\n');// + TextUtil.toHexString(hpposPayload) + '\n');
                        } else if (Arrays.equals(new byte[]{data[3]}, new byte[]{id_Status})) {
                            //receiveText.append("Found stat" + '\n');

                            // WARNING When nav-status was enabled the bluetooth line was overtaken by those messages
                        }
                    }
                }
            }
            //decodes UBX-NAV-HPPOSLLH
            /*
            String flags = String.format("%8s", Integer.toBinaryString(data[9] & 0xFF)).replace(' ', '0');
            long iTOW = getUInt32(new byte[]{data[10], data[(10 + 1)], data[(10 + 2)], data[(10 + 3)]});
            int lon = ((0xFF & data[14 + 3]) << 24) | ((0xFF & data[14 + 2]) << 16) |
                    ((0xFF & data[14 + 1]) << 8) | (0xFF & data[14]);
            int lat = ((0xFF & data[18 + 3]) << 24) | ((0xFF & data[18 + 2]) << 16) |
                    ((0xFF & data[18 + 1]) << 8) | (0xFF & data[18]);
            int height = ((0xFF & data[22 + 3]) << 24) | ((0xFF & data[22 + 2]) << 16) |
                    ((0xFF & data[22 + 1]) << 8) | (0xFF & data[22]);
            int hMSL = ((0xFF & data[26 + 3]) << 24) | ((0xFF & data[26 + 2]) << 16) |
                    ((0xFF & data[26 + 1]) << 8) | (0xFF & data[26]);
            short lonHp = (short) data[34];
            short latHp = (short) data[35];
            short heightHp = (short) data[36];
            short hMSLHp = (short) data[37];
            long hAcc = getUInt32(new byte[]{data[38], data[(38 + 1)], data[(38 + 2)], data[(38 + 3)]});
            // Not sure why I've run out of bytes? Ublox also gets weird readings for vAcc/hAcc
            long vAcc = 0;//getUInt32(new byte[] {data[42],data[(42+1)],data[(42+2)],data[(42+3)]});

             */

            /*receiveText.append("New message " + String.valueOf(data.length) + '\n'
                    + "Flags = [" + flags + "]" + '\n'
                    + "iTOW = [" + String.valueOf(iTOW) + "]" + '\n'
                    + "Lon = [" + String.valueOf(lon) + "]" + '\n'
                    + "Lat = [" + String.valueOf(lat) + "]" + '\n'
                    + "Height = [" + String.valueOf(height) + "]" + '\n'
                    + "hMSL = [" + String.valueOf(hMSL) + "]" + '\n'
                    + "lonHp = [" + String.valueOf(lonHp) + "]" + '\n'
                    + "latHp = [" + String.valueOf(latHp) + "]" + '\n'
                    + "heightHp = [" + String.valueOf(heightHp) + "]" + '\n'
                    + "hMSLHp = [" + String.valueOf(hMSLHp) + "]" + '\n'
                    + "hAcc = [" + String.valueOf(hAcc) + "]" + '\n'
                    + "vAcc = [" + String.valueOf(vAcc) + "]" + '\n' + '\n');

             */




        }
    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

}
