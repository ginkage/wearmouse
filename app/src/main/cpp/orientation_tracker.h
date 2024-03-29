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
#ifndef CARDBOARD_ORIENTATION_TRACKER_H_
#define CARDBOARD_ORIENTATION_TRACKER_H_

#include <array>
#include <memory>
#include <mutex>  // NOLINT

#include "sensors/accelerometer_data.h"
#include "sensors/gyroscope_data.h"
#include "sensors/sensor_event_producer.h"
#include "sensors/sensor_fusion_ekf.h"
#include "sensors/sensor_thread_callbacks.h"
#include "util/rotation.h"

namespace cardboard {

// OrientationTracker encapsulates pose tracking by connecting sensors
// to SensorFusion.
// This pose tracker reports poses in display space.
class OrientationTracker {
 public:
  OrientationTracker(const Vector3& calibration,
                     const int sampling_period_us,
                     SensorThreadCallbacks* callbacks);
  virtual ~OrientationTracker();

  // Pauses tracking and sensors.
  void Pause();

  // Resumes tracking ans sensors.
  void Resume();

  // Gets the predicted pose for a given timestamp.
  Vector4 GetPose(int64_t timestamp_ns) const;

 private:
  // Function called when receiving AccelerometerData.
  //
  // @param event sensor event.
  void OnAccelerometerData(const AccelerometerData& event);

  // Function called when receiving GyroscopeData.
  //
  // @param event sensor event.
  void OnGyroscopeData(const GyroscopeData& event);

  // Registers this as a listener for data from the accel and gyro sensors. This
  // is useful for informing the sensors that they may need to start polling for
  // data.
  void RegisterCallbacks();
  // Unregisters this as a listener for data from the accel and gyro sensors.
  // This is useful for informing the sensors that they may be able to stop
  // polling for data.
  void UnregisterCallbacks();

  long sampling_period_ns_;
  Vector3 calibration_;
  std::unique_ptr<SensorThreadCallbacks> thread_callbacks_;

  std::atomic<bool> is_tracking_;
  // Sensor Fusion object that stores the internal state of the filter.
  std::unique_ptr<SensorFusionEkf> sensor_fusion_;
  // Latest gyroscope data.
  GyroscopeData latest_gyroscope_data_;

  // Event providers supplying AccelerometerData and GyroscopeData to the
  // detector.
  std::shared_ptr<SensorEventProducer<AccelerometerData>> accel_sensor_;
  std::shared_ptr<SensorEventProducer<GyroscopeData>> gyro_sensor_;

  // Callback functions registered to the input SingleTypeEventProducer.
  std::function<void(AccelerometerData)> on_accel_callback_;
  std::function<void(GyroscopeData)> on_gyro_callback_;
};

}  // namespace cardboard

#endif  // CARDBOARD_SDK_ORIENTATION_TRACKER_H_
