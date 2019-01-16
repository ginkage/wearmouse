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
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import java.util.concurrent.TimeUnit;

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
        void onOrientation(float[] quaternion);
    }

    private final IBinder binder = new LocalBinder();
    private OrientationFusion orientation;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        orientation = new OrientationFusion(this);
    }

    @Override
    public void onDestroy() {
        stopInput();
        super.onDestroy();
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
        long samplingPeriodUs = reducedRate ? DATA_RATE_LOW_US : DATA_RATE_HIGH_US;
        orientation.start(listener, TimeUnit.MICROSECONDS.toNanos(samplingPeriodUs));
    }

    /** Stops all sensors interactions. */
    public void stopInput() {
        orientation.stop();
    }
}
