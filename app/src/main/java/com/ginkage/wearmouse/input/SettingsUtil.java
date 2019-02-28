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
import android.content.SharedPreferences;
import androidx.annotation.StringDef;
import com.ginkage.wearmouse.input.MouseSensorListener.HandMode;
import com.google.common.collect.ImmutableMap;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

/** Helper class to wrap various Settings. */
public class SettingsUtil {

    private static final String SETTINGS_PREF = "com.ginkage.wearmouse.SETTINGS";

    /** These constants correspond to the various preferences in the Settings menu. */
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
        SettingKey.MOUSE_HAND,
        SettingKey.CURSOR_8_WAY,
        SettingKey.REDUCED_RATE,
        SettingKey.STABILIZE,
        SettingKey.STAY_CONNECTED
    })
    public @interface SettingKey {
        String MOUSE_HAND = "pref_settingMouseHand";
        String CURSOR_8_WAY = "pref_settingCursor8Way";
        String REDUCED_RATE = "pref_settingReducedRate";
        String STABILIZE = "pref_settingStabilize";
        String STAY_CONNECTED = "pref_settingStayConnected";
    }

    private static final Map<String, Boolean> defaults =
            new ImmutableMap.Builder<String, Boolean>()
                    .put(SettingKey.CURSOR_8_WAY, false)
                    .put(SettingKey.REDUCED_RATE, false)
                    .put(SettingKey.STABILIZE, false)
                    .put(SettingKey.STAY_CONNECTED, false)
                    .build();

    private final SharedPreferences sharedPref;

    /** @param context The context to retrieve shared preferences with. */
    public SettingsUtil(Context context) {
        context = checkNotNull(context).getApplicationContext();
        sharedPref = context.getSharedPreferences(SETTINGS_PREF, Context.MODE_PRIVATE);
    }

    /**
     * Get the last used watch location.
     *
     * @return Watch location (left/right wrist, or held in hand).
     * @see MouseSensorListener
     */
    public @HandMode int getMouseHand() {
        return sharedPref.getInt(SettingKey.MOUSE_HAND, 0);
    }

    /**
     * Save the current watch location.
     *
     * @param hand Watch location (left/right wrist, or held in hand).
     * @see MouseSensorListener
     */
    public void putMouseHand(@HandMode int hand) {
        sharedPref.edit().putInt(SettingKey.MOUSE_HAND, hand).apply();
    }

    /**
     * Gets the boolean value that corresponds to the specified key.
     *
     * @param key Key in the values map.
     * @return Value that corresponds to the key.
     */
    public boolean getBoolean(@SettingKey String key) {
        return sharedPref.getBoolean(key, defaults.get(key));
    }

    /**
     * Saves the boolean value that corresponds to the specified key.
     *
     * @param key Key in the values map.
     * @param enabled Value that corresponds to the key.
     */
    public void setBoolean(@SettingKey String key, boolean enabled) {
        sharedPref.edit().putBoolean(key, enabled).apply();
    }
}
