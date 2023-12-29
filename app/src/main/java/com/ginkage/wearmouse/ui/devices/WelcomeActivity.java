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

import static android.Manifest.permission.BLUETOOTH_ADVERTISE;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.wearable.preference.WearablePreferenceActivity;
import com.ginkage.wearmouse.ui.onboarding.OnboardingController.ScreenKey;
import com.ginkage.wearmouse.ui.onboarding.OnboardingRequest;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/** Main activity that is started from launcher. */
public class WelcomeActivity extends WearablePreferenceActivity {
    private OnboardingRequest onboardingRequest;
    private static final List<String> requiredPermissions = ImmutableList.of(
            BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT, BLUETOOTH_SCAN
    );

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startPreferenceFragment(new PairedDevicesFragment(), false);
                return;
            }
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            List<String> missingPermissions = new ArrayList<>();
            for (String permission : requiredPermissions) {
                if (checkSelfPermission(permission)  != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add(permission);
                }
            }
            if (!missingPermissions.isEmpty()) {
                requestPermissions(missingPermissions.toArray(new String[0]), 1);
                return;
            }
        }

        onboardingRequest = new OnboardingRequest(this, ScreenKey.WELCOME);
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
