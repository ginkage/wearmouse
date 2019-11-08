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

/** JNI for accessing the native sensor fusion implementation. */
class SensorFusionJni {

    private final long nativeSensorFusionPtr;

    static {
        System.loadLibrary("sensor_fusion_jni");
    }

    /** Initializes the native sensor fusion. Must be called before this object can be used. */
    SensorFusionJni(float[] calibration, int samplingPeriodUs) {
        nativeSensorFusionPtr = nativeInit(calibration, samplingPeriodUs);
    }

    /** De-initializes the native sensor fusion. Must be called before releasing this object. */
    synchronized void destroy() {
        nativeDestroy(nativeSensorFusionPtr);
    }

    /**
     * Retrieves the sensor-fused device orientation.
     *
     * @param orientation the output as a quaternion (x, y, z, w)
     * @param timestampNs timestamp of the orientation to predict at
     */
    synchronized void getOrientation(float[] orientation, long timestampNs) {
        nativeGetOrientation(nativeSensorFusionPtr, orientation, timestampNs);
    }

    private native long nativeInit(float[] calibration, int samplingPeriodUs);

    private native void nativeDestroy(long nativeSensorFusionPtr);

    private native void nativeGetOrientation(
            long sensorFusionNativePtr, float[] orientation, long timestampNs);
}
