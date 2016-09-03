package com.eagle.sensorcollector;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Created by hvtran.2014 on 25/5/2015.
 */
public class SensorThread extends Thread implements SensorEventListener{

    private final String INFOTAG = "SENSORTHREAD_INFO";
    private final String ERRTAG = "SENSORTHREAD_ERROR";

    MainActivity parent;
    OutputStreamWriter outputStreamWriter;
    BufferedReader reader;
    private boolean stopRequest;

    public SensorThread(MainActivity activity, OutputStream oStream, InputStream iStream){
        parent = activity;
        outputStreamWriter = new OutputStreamWriter(oStream);
        reader = new BufferedReader(new InputStreamReader(iStream));
        stopRequest = false;
    }

    @Override
    public void run(){
        while(!stopRequest){
            try {
                String line = reader.readLine();
                parent.doCommand(line);
            } catch (IOException e) {
                Log.e(ERRTAG, "Error reading input stream");
                return;
            }
        }
        Log.i(INFOTAG, "Finish thread");
    }

    public void clean(){
        stopRequest = true;
        interrupt();
        try {
            Log.i(INFOTAG, "Before in stream close");
            reader.close();
            Log.i(INFOTAG, "Before out stream close");
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e(ERRTAG, "Error closing streams");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final String message;
        if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
            message = event.sensor.getType() + "," + Long.toString(event.timestamp) + "," + Float.toString(event.values[0]) + "," + Float.toString(event.values[1]) + "," + Float.toString(event.values[2]) + "\n";
        }else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            message = event.sensor.getType() + "," + Long.toString(event.timestamp) + "," + Float.toString(event.values[0]) + "," + Float.toString(event.values[1]) + "," + Float.toString(event.values[2]) + "\n";
        //}else if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
          //  message = event.sensor.getType() + "," + Long.toString(event.timestamp) + "," + Float.toString(event.values[0]) + "," + Float.toString(event.values[1]) + "," + Float.toString(event.values[2]) + "\n";
        }else if(event.sensor.getType() == Sensor.TYPE_GRAVITY){
            message = event.sensor.getType() + "," + Long.toString(event.timestamp) + "," + Float.toString(event.values[0]) + "," + Float.toString(event.values[1]) + "," + Float.toString(event.values[2]) + "\n";
        }else {
            message = "";
        }
        try {
            outputStreamWriter.write(message);
            outputStreamWriter.flush();
        }catch(IOException e) {
            Log.e(ERRTAG, "Error writing output stream");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }
}
