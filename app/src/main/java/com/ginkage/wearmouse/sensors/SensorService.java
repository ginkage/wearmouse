/*
 * Copyright 2018 Google LLC All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ginkage.wearmouse.sensors;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import javax.annotation.Nullable;

/** Service that listens to the sensors events and handles the data. */
public class SensorService extends Service {

    private static final String TAG = "SensorService";

    private static final int DATA_RATE_LOW_US = 20000;
    private static final int DATA_RATE_HIGH_US = 11250;

    /** Interface for binding the service to an activity. */
    class LocalBinder extends Binder {
        /**
         * Get the service instance from the Binder proxy.
         *
         * @return Service instance.
         */
        SensorService getService() {
            return SensorService.this;
        }
    }

    /** Interface for subscribing to the orientation changes. */
    public interface OrientationListener {
        /**
         * Returns the current device orientation, in the same way as Game Rotation Vector does.
         *
         * @param quaternion Device orientation.
         */
        void onOrientation(double[] quaternion);
    }

    /** Callback to be notified of the calibration completion. */
    public interface CalibrationListener {
        /**
         * Called when we have collected enough sensor data for gyroscope calibration.
         *
         * @param success {@code true} if calibration completed successfully, {@code false}
         *     otherwise.
         */
        void onCalibrationComplete(boolean success);
    }

    private final IBinder binder = new LocalBinder();

    private final SensorEventListener sensorEventListener =
            new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    if (!registered) {
                        return;
                    }

                    switch (event.sensor.getType()) {
                        case Sensor.TYPE_GYROSCOPE:
                        case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                            if (calibrating && calibrationData.add(event.values)) {
                                if (calibrationListener != null) {
                                    calibrationListener.onCalibrationComplete(true);
                                }
                                stopInput();
                            }
                            break;
                        default: // fall out
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {}
            };

    private SensorManager sensorManager;
    private OrientationFusion orientation;

    private boolean calibrating;
    private boolean registered;
    private CalibrationData calibrationData;
    @Nullable private CalibrationListener calibrationListener;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        registered = false;
        calibrating = false;
        calibrationData = new CalibrationData(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        orientation = new OrientationFusion();
    }

    @Override
    public void onDestroy() {
        stopInput();
        super.onDestroy();
    }

    /**
     * Starts collecting the gyroscope data for calibration.
     *
     * @param listener Callback to be notified when the calibration is complete.
     */
    public void startCalibration(CalibrationListener listener) {
        stopInput();

        calibrationListener = listener;

        Sensor sensorGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
        if (sensorGyroscope == null) {
            sensorGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
        if (sensorGyroscope == null
                || !sensorManager.registerListener(sensorEventListener, sensorGyroscope, 10000)) {
            Log.e(TAG, "Failed to register listener for Gyroscope: " + sensorGyroscope);
            if (listener != null) {
                listener.onCalibrationComplete(false);
            }
            return;
        }

        calibrationData.reset();
        calibrating = true;
        registered = true;
    }

    /**
     * Starts listening for the sensors data and providing the device orientation.
     *
     * @param listener Callback to receive the device orientation.
     * @param reducedRate {@code true} if orientation events should be provided at 50 Hz, {@code
     *     false} for 88.89 Hz.
     */
    public void startInput(OrientationListener listener, boolean reducedRate) {
        stopInput();
        int samplingPeriodUs = reducedRate ? DATA_RATE_LOW_US : DATA_RATE_HIGH_US;
        orientation.start(listener, samplingPeriodUs, calibrationData.getMedian());
    }

    /** Stops all sensors interactions. */
    public void stopInput() {
        if (registered) {
            registered = false;
            calibrating = false;
            sensorManager.unregisterListener(sensorEventListener);
        }

        orientation.stop();

        calibrationListener = null;
    }
}
