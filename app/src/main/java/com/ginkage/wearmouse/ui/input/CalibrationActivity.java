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

package com.ginkage.wearmouse.ui.input;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import com.ginkage.wearmouse.R;
import com.ginkage.wearmouse.input.CalibrationController;

/** Show a spinner animation while the gyroscope is being calibrated. */
public class CalibrationActivity extends WearableActivity {

    private CalibrationController calibrationController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAmbientEnabled();
        setContentView(R.layout.activity_calibration);

        calibrationController = new CalibrationController(this, this::onCalibrationComplete);
        calibrationController.onCreate();
    }

    @Override
    public void onResume() {
        super.onResume();
        calibrationController.startCalibration();
    }

    @Override
    public void onPause() {
        calibrationController.stopCalibration();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        calibrationController.onDestroy();
        super.onDestroy();
    }

    private void onCalibrationComplete(boolean success) {
        setResult(success ? RESULT_OK : RESULT_CANCELED);
        finish();
    }
}
