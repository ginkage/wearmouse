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

class JNIThreadCallbacks : public cardboard::SensorThreadCallbacks {
 public:
  JNIThreadCallbacks(JNIEnv* env, jobject obj) {
    env->GetJavaVM(&jvm_);
    obj_ = env->NewGlobalRef(obj);
    dst_orientation_ = reinterpret_cast<jdoubleArray>(
        env->NewGlobalRef(env->NewDoubleArray(4)));

    jclass clazz =
        env->FindClass("com/ginkage/wearmouse/sensors/SensorFusionJni");
    method_on_orientation_ = env->GetMethodID(clazz, "onOrientation", "([D)V");
  }

  void onThreadStart() override {
    JavaVMAttachArgs args = {
        .version = JNI_VERSION_1_6, .name = nullptr, .group = nullptr};
    jvm_->AttachCurrentThread(&env_, &args);
    running_ = true;
  }

  void onOrientation(const cardboard::Vector4& quat) override {
    if (running_) {
      env_->SetDoubleArrayRegion(dst_orientation_, 0, 4,
                                 reinterpret_cast<const jdouble*>(&quat));
      env_->CallVoidMethod(obj_, method_on_orientation_, dst_orientation_);
    }
  }

  void onThreadStop() override {
    running_ = false;
    env_->DeleteGlobalRef(dst_orientation_);
    env_->DeleteGlobalRef(obj_);
    jvm_->DetachCurrentThread();
  }

 private:
  bool running_ = false;
  JavaVM* jvm_ = nullptr;
  JNIEnv* env_ = nullptr;
  jdoubleArray dst_orientation_;
  jmethodID method_on_orientation_;
  jobject obj_;
};

}  // anonymous namespace

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
  return JNI_VERSION_1_6;
}

JNI_METHOD(jlong, nativeInit)
(JNIEnv* env, jobject obj, jdoubleArray calibration, jint sampling_period_us) {
  cardboard::Vector3 bias;
  env->GetDoubleArrayRegion(calibration, 0, 3,
                            reinterpret_cast<jdouble*>(&bias));

  auto tracker = new cardboard::OrientationTracker(
      bias, sampling_period_us, new JNIThreadCallbacks(env, obj));
  tracker->Resume();
  return jptr(tracker);
}

JNI_METHOD(void, nativeDestroy)
(JNIEnv* env, jobject obj, jlong native_app) {
  cardboard::OrientationTracker* tracker = native(native_app);
  tracker->Pause();
  delete tracker;
}

}  // extern "C"
