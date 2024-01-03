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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import com.ginkage.wearmouse.R;
import com.ginkage.wearmouse.input.SettingsUtil;
import com.ginkage.wearmouse.input.SettingsUtil.SettingKey;
import com.ginkage.wearmouse.ui.onboarding.OnboardingController;
import com.ginkage.wearmouse.ui.onboarding.OnboardingController.ScreenKey;
import com.ginkage.wearmouse.ui.onboarding.OnboardingRequest;

/** The main Settings fragment. */
public class InputSettingsFragment extends PreferenceFragment {
    private static final String ONBOARDING_PREF = "pref_settingReplayTutorials";
    private static final int CALIBRATION_REQUEST_CODE = 1;

    private SettingsUtil settings;
    private SwitchPreference calibrationPref;
    private OnboardingRequest onboardingRequest;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
        try {
            addPreferencesFromResource(R.xml.prefs_input_settings);
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }

        settings = new SettingsUtil(getActivity());
        onboardingRequest = new OnboardingRequest(getActivity(), ScreenKey.CALIBRATION);
        calibrationPref = (SwitchPreference) findPreference(SettingKey.CALIBRATION);

        initBooleanPref(SettingKey.STABILIZE);
        initBooleanPref(SettingKey.CURSOR_8_WAY);
        initBooleanPref(SettingKey.REDUCED_RATE);
        initBooleanPref(SettingKey.STAY_CONNECTED);

        updateCalibrationPref();
        calibrationPref.setOnPreferenceChangeListener(
                (p, newVal) -> {
                    settings.setBoolean(SettingKey.CALIBRATION, false);
                    onboardingRequest.start(this);
                    return true;
                });

        OnboardingController onboardingController = new OnboardingController(getActivity());
        Preference onboardingPref = findPreference(ONBOARDING_PREF);
        onboardingPref.setEnabled(onboardingController.isAnyComplete());
        onboardingPref.setOnPreferenceClickListener(
                preference -> {
                    if (preference.isEnabled()) {
                        onboardingController.resetAll();
                        preference.setEnabled(false);
                    }
                    return true;
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        getView().requestFocus();
    }

    private void initBooleanPref(@SettingKey final String key) {
        final SwitchPreference pref = (SwitchPreference) findPreference(key);
        pref.setChecked(settings.getBoolean(key));
        pref.setOnPreferenceChangeListener(
                (p, newVal) -> {
                    settings.setBoolean(p.getKey(), ((Boolean) newVal));
                    return true;
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (onboardingRequest.isMyResult(requestCode, data) && resultCode == Activity.RESULT_OK) {
            startActivityForResult(
                    new Intent(getActivity(), CalibrationActivity.class), CALIBRATION_REQUEST_CODE);
        } else if (requestCode == CALIBRATION_REQUEST_CODE) {
            settings.setBoolean(SettingKey.CALIBRATION, resultCode == Activity.RESULT_OK);
        }
        updateCalibrationPref();
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateCalibrationPref() {
        boolean calibrated = settings.getBoolean(SettingKey.CALIBRATION);
        calibrationPref.setChecked(calibrated);
        calibrationPref.setTitle(
                calibrated ? R.string.setting_recalibrate : R.string.pref_settingCalibration);
    }
}
