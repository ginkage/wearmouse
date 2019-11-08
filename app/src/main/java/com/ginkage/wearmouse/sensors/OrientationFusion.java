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

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.GuardedBy;
import androidx.annotation.WorkerThread;
import com.ginkage.wearmouse.sensors.SensorService.OrientationListener;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/**
 * A module that sends the sensor-fused absolute orientation (pitch, yaw, roll) of the watch with a
 * specified update period.
 */
class OrientationFusion {

    private static final String TAG = "OrientationFusion";

    private final Object lock = new Object();

    @GuardedBy("lock")
    @Nullable
    private Tracker tracker;

    /**
     * Starts listening to the sensors and providing the orientation data.
     *
     * @param listener the callback to receive the orientation data.
     * @param samplingPeriodUs the period between the sensors readings in microseconds.
     */
    void start(OrientationListener listener, int samplingPeriodUs, Vector calibrationData) {
        synchronized (lock) {
            if (tracker == null) {
                tracker =
                        new Tracker(
                                listener,
                                TimeUnit.MICROSECONDS.toNanos(samplingPeriodUs),
                                new SensorFusionJni(
                                        new float[] {
                                                (float) calibrationData.x,
                                                (float) calibrationData.y,
                                                (float) calibrationData.z
                                        },
                                        samplingPeriodUs),
                                new ScheduledThreadPoolExecutor(1));
                tracker.schedule(this::processOrientation);
            }
        }
    }

    /** Stops listening to the sensors. */
    void stop() {
        synchronized (lock) {
            if (tracker != null) {
                tracker.shutdown();
                tracker = null;
            }
        }
    }

    private void processOrientation() {
        synchronized (lock) {
            if (tracker != null) {
                tracker.processOrientation();
            }
        }
    }

    /** Encapsulates everything that should be protected with a lock. */
    private static class Tracker {
        private final OrientationListener orientationListener;
        private final ScheduledThreadPoolExecutor executor;
        private final SensorFusionJni sensorFusionJni;
        private final long samplingPeriodNs;
        private final float[] quat = new float[4];

        @Nullable private ScheduledFuture<?> scheduledFuture;

        private Tracker(
                OrientationListener orientationListener,
                long samplingPeriodNs,
                SensorFusionJni sensorFusionJni,
                ScheduledThreadPoolExecutor executor) {
            this.orientationListener = checkNotNull(orientationListener);
            this.samplingPeriodNs = samplingPeriodNs;
            this.sensorFusionJni = checkNotNull(sensorFusionJni);
            this.executor = checkNotNull(executor);
        }

        void schedule(Runnable command) {
            if (scheduledFuture == null) {
                scheduledFuture =
                        executor.scheduleAtFixedRate(
                                command, 0, samplingPeriodNs, TimeUnit.NANOSECONDS);
            }
        }

        void shutdown() {
            if (scheduledFuture != null) {
                executor.shutdownNow();
                scheduledFuture.cancel(true);
                scheduledFuture = null;
                sensorFusionJni.destroy();
            }
        }

        @WorkerThread
        void processOrientation() {
            sensorFusionJni.getOrientation(quat, System.nanoTime() + samplingPeriodNs);
            orientationListener.onOrientation(quat);
        }
    }
}
