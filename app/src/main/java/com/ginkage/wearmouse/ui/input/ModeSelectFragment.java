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
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.wearable.preference.WearablePreferenceActivity;
import com.ginkage.wearmouse.R;
import com.ginkage.wearmouse.input.KeyboardInputController;
import com.ginkage.wearmouse.ui.input.InputActivity.InputMode;

/** Main menu for choosing the input mode. Also handles the Keyboard Input mode. */
public class ModeSelectFragment extends PreferenceFragment {

    private static final String KEY_PREF_INPUT_MOUSE = "pref_inputMouse";
    private static final String KEY_PREF_INPUT_TOUCHPAD = "pref_inputTouchpad";
    private static final String KEY_PREF_INPUT_CURSOR = "pref_inputCursor";
    private static final String KEY_PREF_INPUT_KEYBOARD = "pref_inputKeyboard";
    private static final int INPUT_REQUEST_CODE = 1;

    private KeyboardInputController keyboardController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_mode_select);

        WearablePreferenceActivity activity = ((WearablePreferenceActivity) getActivity());
        activity.setAmbientEnabled();

        assignIntent(KEY_PREF_INPUT_MOUSE, InputMode.MOUSE);
        assignIntent(KEY_PREF_INPUT_TOUCHPAD, InputMode.TOUCHPAD);
        assignIntent(KEY_PREF_INPUT_CURSOR, InputMode.KEYPAD);

        keyboardController = new KeyboardInputController(activity::finish);
        keyboardController.onCreate(getContext());

        Preference keyboardPref = findPreference(KEY_PREF_INPUT_KEYBOARD);
        Intent intent = keyboardController.getInputIntent(getContext().getPackageManager());
        if (intent == null) {
            keyboardPref.setEnabled(false);
        } else {
            keyboardPref.setOnPreferenceClickListener(
                    (p) -> {
                        startActivityForResult(intent, INPUT_REQUEST_CODE);
                        return true;
                    });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        keyboardController.onResume();
    }

    @Override
    public void onDestroy() {
        keyboardController.onDestroy(getContext());
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == INPUT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            keyboardController.onActivityResult(data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void assignIntent(String prefKey, int inputMode) {
        findPreference(prefKey)
                .setIntent(
                        new Intent(getActivity(), InputActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                .putExtra(InputActivity.EXTRA_INPUT_MODE, inputMode));
    }
}
