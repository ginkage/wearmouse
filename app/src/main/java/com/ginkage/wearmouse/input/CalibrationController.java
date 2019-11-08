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

package com.ginkage.wearmouse.input;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;
import com.ginkage.wearmouse.sensors.SensorService;
import com.ginkage.wearmouse.sensors.SensorServiceConnection;
import javax.annotation.Nullable;

/** Controls the calibration sequence handling for the corresponding UI. */
public class CalibrationController {

  /** Callback for the UI. */
  public interface Ui extends SensorService.CalibrationListener {}

  private final Ui ui;
  private final SensorServiceConnection connection;
  private boolean scheduledCalibration;

  /**
   * @param context Activity the this controller is bound to.
   * @param ui Callback for receiving the UI updates.
   */
  public CalibrationController(Context context, Ui ui) {
    this.ui = checkNotNull(ui);
    this.connection =
        new SensorServiceConnection(
            checkNotNull(context),
            service -> {
              if (scheduledCalibration) {
                service.startCalibration(this.ui);
              }
            });
  }

  /** Should be called in the Activity's (or Fragment's) onCreate() method. */
  public void onCreate() {
    connection.bind();
  }

  /** Should be called in the Activity's (or Fragment's) onDestroy() method. */
  public void onDestroy() {
    connection.unbind();
  }

  /**
   * Start the calibration sequence if the sensor service is bound, or schedule it to start when it
   * is available.
   */
  public void startCalibration() {
    @Nullable SensorService service = connection.getService();
    if (service != null) {
      service.startCalibration(ui);
    } else {
      scheduledCalibration = true;
    }
  }

  public void stopCalibration() {
    @Nullable SensorService service = connection.getService();
    if (service != null) {
      service.stopInput();
    }
    scheduledCalibration = false;
  }
}
