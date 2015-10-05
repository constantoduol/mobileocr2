package com.quest.mobileocr;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/**
 * Created by Connie on 02-Jun-15.
 */
public class Accelerometer implements SensorEventListener {

    private float[] gravity = new float[3];

    private float[] linear_acceleration = new float[3];

    private final static float STILLNESS_FACTOR = 0.05F;


    @Override
    public void onSensorChanged(SensorEvent event) {
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.

        final float alpha = 0.8f;

        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //ignore for now
    }

    public boolean isStill(){
       return Math.abs(linear_acceleration[0]) < STILLNESS_FACTOR
               && Math.abs(linear_acceleration[1]) < STILLNESS_FACTOR
               && Math.abs(linear_acceleration[2]) < STILLNESS_FACTOR;
    }
}
