package com.eagle.sensorcollector;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    private static final String MY_UUID = "852159da-a17b-4057-983d-830c4537851c";
    private final int REQUEST_ENABLE_BT = 1;
    private final int SAMPLING_RATE_HS = 10000;
    private final int SAMPLING_RATE_LS = 20000;
    private final String INFOTAG = "SENSORCOLLECTOR";
    private final String ERRTAG = "SENSORCOLLECTOR";

    private Button btStartStop = null;
    private Spinner spinDevices = null;
    private Spinner spinConfigs = null;
    private TextView tvStatus = null;
    private Context context = this;
    private SensorManager sensorManager = null;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothSocket bluetoothSocket = null;
    private SensorThread sensorThread = null;
    private boolean started = false;
    private int deviceSelected = 0;
    private int configSelected = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(INFOTAG, "Testtttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttt");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                btStartStop = (Button) stub.findViewById(R.id.btStartStop);
                btStartStop.setBackground(getResources().getDrawable(R.drawable.btplay, null));
                spinDevices = (Spinner) stub.findViewById(R.id.spinDevices);
                spinConfigs = (Spinner) stub.findViewById(R.id.spinConfigs);
                tvStatus = (TextView) stub.findViewById(R.id.tvStatus);
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                ArrayList<String> listDevices = new ArrayList<String>();
                for (int i = 0; i < pairedDevices.size(); i++) {
                    listDevices.add(((BluetoothDevice) pairedDevices.toArray()[i]).getName());
                }
                ArrayAdapter<String> spinDevicesAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, listDevices);
                spinDevices.setAdapter(spinDevicesAdapter);
                ArrayList<String> listConfigs = new ArrayList<String>();
                listConfigs.add("All three");
                listConfigs.add("Accelerometer");
                listConfigs.add("Gyroscope");
                listConfigs.add("Gravity");
                ArrayAdapter<String> spinConfigsAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, listConfigs);
                spinConfigs.setAdapter(spinConfigsAdapter);
            }
        });
    }

    public void doCommand(String cmd){
        tvStatus.setText(cmd);
    }

    public void onBtStartStopClick(View view){
        Log.i(INFOTAG, "Button clicked");
        if(started){
            tvStatus.setText("Disconnecting");
            stopSensor();
            try {
                bluetoothSocket.close();
            }catch (IOException e){
                Log.e(INFOTAG, "Error closing socket");
            }
            try {
                Log.i(INFOTAG, "Before clean");
                sensorThread.clean();
                Log.i(INFOTAG, "Before join");
                sensorThread.join();
                Log.i(INFOTAG, "After join");
            }catch (InterruptedException e){
                Log.e(INFOTAG, "Error joining thread");
            }
            sensorThread = null;
            btStartStop.setBackground(getResources().getDrawable(R.drawable.btplay, null));
            started = false;
            tvStatus.setText("Disconnected");
        }else {
            tvStatus.setText("Connecting");
            deviceSelected = spinDevices.getSelectedItemPosition();
            configSelected = spinConfigs.getSelectedItemPosition();
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            BluetoothDevice device = ((BluetoothDevice) pairedDevices.toArray()[deviceSelected]);
            bluetoothSocket = connectBluetooth(device);
            if(bluetoothSocket != null) {
                OutputStream oStream = null;
                InputStream iStream = null;
                try {
                    oStream = bluetoothSocket.getOutputStream();
                    iStream = bluetoothSocket.getInputStream();
                    sensorThread = new SensorThread(this, oStream, iStream);
                    sensorThread.start();
                    startSensor();
                    btStartStop.setBackground(getResources().getDrawable(R.drawable.btstop, null));
                    started = true;
                    tvStatus.setText("Connected");
                } catch (IOException e) {
                    tvStatus.setText("Cannot connect");
                    Log.e(ERRTAG, "Error getting in/out stream");
                }
            } else {
                tvStatus.setText("Cannot connect");
            }
        }
    }

    private BluetoothSocket connectBluetooth(BluetoothDevice device){
        BluetoothSocket socket = null;
        try {
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
            if(socket != null) {
                socket.connect();
                return socket;
            }else {
                Log.e(INFOTAG, "Error creating socket");
                return null;
            }
        } catch (IOException e) {
            Log.e(INFOTAG, "Exception creating socket");
            return null;
        }
    }

    private void startSensor(){
        if((configSelected == 0) || (configSelected == 1)) sensorManager.registerListener(sensorThread, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SAMPLING_RATE_LS);
        if((configSelected == 0) || (configSelected == 2)) sensorManager.registerListener(sensorThread, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SAMPLING_RATE_LS);
        if((configSelected == 0) || (configSelected == 3)) sensorManager.registerListener(sensorThread, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SAMPLING_RATE_LS);
    }

    private void stopSensor(){
        if((configSelected == 0) || (configSelected == 1)) sensorManager.unregisterListener(sensorThread, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
        if((configSelected == 0) || (configSelected == 2)) sensorManager.unregisterListener(sensorThread, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
        if((configSelected == 0) || (configSelected == 3)) sensorManager.unregisterListener(sensorThread, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY));
    }
}
