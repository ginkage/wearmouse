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

import android.content.Context;
import android.support.annotation.GuardedBy;
import android.support.annotation.WorkerThread;
import com.ginkage.wearmouse.sensors.SensorService.OrientationListener;
import com.google.vr.ndk.base.GvrApi;
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

    private final Context context;
    private final Object lock = new Object();

    @GuardedBy("lock")
    @Nullable
    private Tracker tracker;

    /**
     * @param context Context for creating a GVR API instance
     */
    OrientationFusion(Context context) {
        this.context = checkNotNull(context);
    }

    /**
     * Starts listening to the sensors and providing the orientation data.
     *
     * @param listener the callback to receive the orientation data.
     * @param samplingPeriodNs the period between the sensors readings in nanoseconds.
     */
    void start(OrientationListener listener, long samplingPeriodNs) {
        synchronized (lock) {
            if (tracker == null) {
                tracker = new Tracker(
                        listener,
                        samplingPeriodNs,
                        new GvrApi(context, null),
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
        private final GvrApi gvrApi;
        private final long samplingPeriodNs;
        private final float[] mat = new float[16];
        private final float[] quat = new float[4];

        @Nullable
        private ScheduledFuture<?> scheduledFuture;

        private Tracker(
                OrientationListener orientationListener,
                long samplingPeriodNs,
                GvrApi gvrApi,
                ScheduledThreadPoolExecutor executor) {
            this.orientationListener = checkNotNull(orientationListener);
            this.samplingPeriodNs = samplingPeriodNs;
            this.gvrApi = checkNotNull(gvrApi);
            this.executor = checkNotNull(executor);
        }

        void schedule(Runnable command) {
            if (scheduledFuture == null) {
                scheduledFuture = executor.scheduleAtFixedRate(
                        command,
                        0,
                        samplingPeriodNs,
                        TimeUnit.NANOSECONDS);
            }
        }

        void shutdown() {
            if (scheduledFuture != null) {
                executor.shutdownNow();
                scheduledFuture.cancel(true);
                scheduledFuture = null;
                gvrApi.shutdown();
            }
        }

        @WorkerThread
        void processOrientation() {
            // Head transform is defined as:
            // head = -(sensor_to_display * predicted_rotation * -default_orientation)
            // where (in quaternion form)
            // -default_orientation = { 0.5, -0.5, -0.5, 0.5 }
            // sensor_to_display = { 0, 0, 0.7071067811865, 0.7071067811865 }
            // So, if we need a "-predicted_rotation" value, then the rotation we obtain from GVR
            // should be transformed as:
            // -predicted_rotation = -default_orientation * head * sensor_to_display
            gvrApi.getHeadSpaceFromStartSpaceTransform(mat, System.nanoTime() + samplingPeriodNs);

            // We do this transformation simultaneously with converting the matrix to quaternion,
            // because transformation in matrix form is much simpler, and it saves a ton of maths.
            double d0 = -mat[9];
            double d1 = mat[0];
            double d2 = mat[6];
            double ww = 1 + d0 + d1 + d2;
            double xx = 1 + d0 - d1 - d2;
            double yy = 1 - d0 + d1 - d2;
            double zz = 1 - d0 - d1 + d2;
            double max = Math.max(ww, Math.max(xx, Math.max(yy, zz)));
            double x;
            double y;
            double z;
            double w;

            if (ww == max) {
                double w4 = Math.sqrt(ww * 4);
                x = (-mat[4] + mat[2]) / w4;
                y = (-mat[10] - mat[5]) / w4;
                z = (-mat[1] - mat[8]) / w4;
                w = w4 / 4;
            } else if (xx == max) {
                double x4 = Math.sqrt(xx * 4);
                x = x4 / 4;
                y = (mat[8] - mat[1]) / x4;
                z = (-mat[10] + mat[5]) / x4;
                w = (-mat[4] + mat[2]) / x4;
            } else if (yy == max) {
                double y4 = Math.sqrt(yy * 4);
                x = (mat[8] - mat[1]) / y4;
                y = y4 / 4;
                z = (-mat[2] - mat[4]) / y4;
                w = (-mat[10] - mat[5]) / y4;
            } else { // zz is the largest component.
                double z4 = Math.sqrt(zz * 4);
                x = (-mat[10] + mat[5]) / z4;
                y = (-mat[2] - mat[4]) / z4;
                z = z4 / 4;
                w = (-mat[1] - mat[8]) / z4;
            }

            quat[0] = (float) x;
            quat[1] = (float) y;
            quat[2] = (float) z;
            quat[3] = (float) w;

            orientationListener.onOrientation(quat);
        }
    }
}
