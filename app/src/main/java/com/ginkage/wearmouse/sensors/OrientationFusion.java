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

import com.ginkage.wearmouse.sensors.SensorService.OrientationListener;

/**
 * A module that sends the sensor-fused absolute orientation quaternion of the watch with a
 * specified update period.
 */
class OrientationFusion {

    private static final String TAG = "OrientationFusion";

    private SensorFusionJni tracker;

    /**
     * Starts listening to the sensors and providing the orientation data.
     *
     * @param listener the callback to receive the orientation data.
     * @param samplingPeriodUs the period between the sensors readings in microseconds.
     */
    void start(OrientationListener listener, int samplingPeriodUs, Vector calibrationData) {
        if (tracker == null) {
            tracker =
                new SensorFusionJni(
                    new double[]{ calibrationData.x, calibrationData.y, calibrationData.z },
                    samplingPeriodUs,
                    listener);
        }
    }

    /** Stops listening to the sensors. */
    void stop() {
        if (tracker != null) {
            tracker.destroy();
            tracker = null;
        }
    }
}
