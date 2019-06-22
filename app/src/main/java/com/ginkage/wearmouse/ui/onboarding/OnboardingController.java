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

package com.ginkage.wearmouse.ui.onboarding;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.StringDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Stores the "completed" state of various tutorials. */
public class OnboardingController {
    private static final String ONBOARDING_PREF = "com.ginkage.wearmouse.ONBOARDING";
    static final int ONBOARDING_REQUEST_CODE = 16;
    static final String EXTRA_KEY = "extra_onboarding_key";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({ScreenKey.WELCOME, ScreenKey.MOUSE, ScreenKey.KEYPAD})
    public @interface ScreenKey {
        String WELCOME = "welcome";
        String MOUSE = "mouse";
        String KEYPAD = "keypad";
    }

    private final SharedPreferences sharedPref;

    /** @param context The context to retrieve shared preferences with. */
    public OnboardingController(Context context) {
        context = checkNotNull(context).getApplicationContext();
        sharedPref = context.getSharedPreferences(ONBOARDING_PREF, Context.MODE_PRIVATE);
    }

    /**
     * Checks if any of the tutorials has been completed.
     *
     * @return {@code true} if at least one tutorial was completed, {@code false} otherwise.
     */
    public boolean isAnyComplete() {
        return isComplete(ScreenKey.WELCOME)
                || isComplete(ScreenKey.MOUSE)
                || isComplete(ScreenKey.KEYPAD);
    }

    /** Forces all tutorials to the default state. */
    public void resetAll() {
        reset(ScreenKey.WELCOME);
        reset(ScreenKey.MOUSE);
        reset(ScreenKey.KEYPAD);
    }

    boolean isComplete(@ScreenKey String key) {
        return sharedPref.getBoolean(key, false);
    }

    void setComplete(@ScreenKey String key) {
        sharedPref.edit().putBoolean(key, true).apply();
    }

    private void reset(@ScreenKey String key) {
        sharedPref.edit().remove(key).apply();
    }
}
