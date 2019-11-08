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
#include "orientation_tracker.h"

#include "sensors/pose_prediction.h"
#include "util/logging.h"
#include "util/vector.h"
#include "util/vectorutils.h"

namespace cardboard {

OrientationTracker::OrientationTracker(const Vector3& calibration,
                                       const int sampling_period_us)
    : calibration_(calibration),
      is_tracking_(false),
      sensor_fusion_(new SensorFusionEkf()),
      latest_gyroscope_data_({0, 0, Vector3::Zero()}),
      accel_sensor_(
          new SensorEventProducer<AccelerometerData>(sampling_period_us)),
      gyro_sensor_(new SensorEventProducer<GyroscopeData>(sampling_period_us)) {
  sensor_fusion_->SetBiasEstimationEnabled(/*kGyroBiasEstimationEnabled*/ true);
  on_accel_callback_ = [&](const AccelerometerData& event) {
    OnAccelerometerData(event);
  };
  on_gyro_callback_ = [&](const GyroscopeData& event) {
    OnGyroscopeData(event);
  };
}

OrientationTracker::~OrientationTracker() { UnregisterCallbacks(); }

void OrientationTracker::Pause() {
  if (!is_tracking_) {
    return;
  }

  UnregisterCallbacks();

  // Create a gyro event with zero velocity. This effectively stops the
  // prediction.
  GyroscopeData event = latest_gyroscope_data_;
  event.data = Vector3::Zero();

  OnGyroscopeData(event);

  is_tracking_ = false;
}

void OrientationTracker::Resume() {
  is_tracking_ = true;
  RegisterCallbacks();
}

const Vector4& OrientationTracker::GetPose(int64_t timestamp_ns) const {
  Rotation predicted_rotation;
  const PoseState pose_state = sensor_fusion_->GetLatestPoseState();
  if (!sensor_fusion_->IsFullyInitialized()) {
    CARDBOARD_LOGI(
        "Orientation Tracker not fully initialized yet. Using pose prediction only.");
    predicted_rotation = pose_prediction::PredictPose(timestamp_ns, pose_state);
  } else {
    predicted_rotation = pose_state.sensor_from_start_rotation;
  }

  return predicted_rotation.GetQuaternion();
}

void OrientationTracker::RegisterCallbacks() {
  accel_sensor_->StartSensorPolling(&on_accel_callback_);
  gyro_sensor_->StartSensorPolling(&on_gyro_callback_);
}

void OrientationTracker::UnregisterCallbacks() {
  accel_sensor_->StopSensorPolling();
  gyro_sensor_->StopSensorPolling();
}

void OrientationTracker::OnAccelerometerData(const AccelerometerData& event) {
  if (!is_tracking_) {
    return;
  }
  sensor_fusion_->ProcessAccelerometerSample(event);
}

void OrientationTracker::OnGyroscopeData(const GyroscopeData& event) {
  if (!is_tracking_) {
    return;
  }

  const GyroscopeData data = {.data = event.data - calibration_,
                              .system_timestamp = event.system_timestamp,
                              .sensor_timestamp_ns = event.sensor_timestamp_ns};

  latest_gyroscope_data_ = data;
  sensor_fusion_->ProcessGyroscopeSample(data);
}

}  // namespace cardboard
