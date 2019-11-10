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

import com.ginkage.wearmouse.sensors.SensorService.OrientationListener;

/** JNI for accessing the native sensor fusion implementation. */
class SensorFusionJni {

    private final long nativeSensorFusionPtr;
    private final OrientationListener listener;
    private final double[] orientation = new double[4];

    static {
        System.loadLibrary("sensor_fusion_jni");
    }

    /** Initializes the native sensor fusion. Must be called before this object can be used. */
    SensorFusionJni(double[] calibration, int samplingPeriodUs, OrientationListener listener) {
        nativeSensorFusionPtr = nativeInit(calibration, samplingPeriodUs);
        this.listener = checkNotNull(listener);
    }

    /** De-initializes the native sensor fusion. Must be called before releasing this object. */
    void destroy() {
        nativeDestroy(nativeSensorFusionPtr);
    }

    /** Called from the native thread whenever new gyroscope sensor data is available. */
    void onOrientation() {
        listener.onOrientation(orientation);
    }

    private native long nativeInit(double[] calibration, int samplingPeriodUs);

    private native void nativeDestroy(long nativeSensorFusionPtr);
}
