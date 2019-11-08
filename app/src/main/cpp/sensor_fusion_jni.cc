/*
 * Copyright 2019 Google Inc. All Rights Reserved.
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

#include <android/log.h>
#include <jni.h>

#include "orientation_tracker.h"

#define JNI_METHOD(return_type, method_name) \
  JNIEXPORT return_type JNICALL              \
      Java_com_ginkage_wearmouse_sensors_SensorFusionJni_##method_name

namespace {

inline jlong jptr(cardboard::OrientationTracker* native_app) {
  return reinterpret_cast<intptr_t>(native_app);
}

inline cardboard::OrientationTracker* native(jlong ptr) {
  return reinterpret_cast<cardboard::OrientationTracker*>(ptr);
}

}  // anonymous namespace

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
  return JNI_VERSION_1_6;
}

JNI_METHOD(jlong, nativeInit)
(JNIEnv* env, jobject obj, jfloatArray calibration, jint sampling_period_us) {
  float *tmp = env->GetFloatArrayElements(calibration, nullptr);
  const cardboard::Vector3 bias(tmp[0], tmp[1], tmp[2]);
  env->ReleaseFloatArrayElements(calibration, tmp, 0);

  auto tracker = new cardboard::OrientationTracker(bias, sampling_period_us);
  tracker->Resume();
  return jptr(tracker);
}

JNI_METHOD(void, nativeDestroy)
(JNIEnv* env, jobject obj, jlong native_app) {
    cardboard::OrientationTracker* tracker = native(native_app);
    tracker->Pause();
    delete tracker;
}

JNI_METHOD(void, nativeGetOrientation)
(JNIEnv* env, jobject obj, jlong native_app, jfloatArray orientation, jlong timestamp_ns) {
  float *out = env->GetFloatArrayElements(orientation, nullptr);
  cardboard::Vector4 quat = native(native_app)->GetPose(timestamp_ns);
  out[0] = static_cast<float>(-quat[0]);
  out[1] = static_cast<float>(-quat[1]);
  out[2] = static_cast<float>(-quat[2]);
  out[3] = static_cast<float>(quat[3]);
  env->ReleaseFloatArrayElements(orientation, out, 0);
}

}  // extern "C"
