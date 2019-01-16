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

package com.ginkage.wearmouse.ui.devices;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.preference.WearablePreferenceActivity;
import com.ginkage.wearmouse.ui.onboarding.OnboardingController;
import com.ginkage.wearmouse.ui.onboarding.OnboardingRequest;

/** Main activity that is started from launcher. */
public class WelcomeActivity extends WearablePreferenceActivity {
    private OnboardingRequest onboardingRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onboardingRequest = new OnboardingRequest(this, OnboardingController.OB_WELCOME);
        if (onboardingRequest.isComplete()) {
            startPreferenceFragment(new PairedDevicesFragment(), false);
        } else {
            onboardingRequest.start();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (onboardingRequest.isMyResult(requestCode, data)) {
            if (resultCode == RESULT_OK) {
                onboardingRequest.setComplete();
                startPreferenceFragment(new PairedDevicesFragment(), true);
            }
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
