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

import android.content.Context;
import android.content.Intent;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import com.ginkage.wearmouse.R;
import com.ginkage.wearmouse.ui.onboarding.OnboardingController.ScreenKey;
import com.google.common.collect.ImmutableMap;
import javax.annotation.Nullable;

/** Simple wrapper around resources required to fill and show {@link OnboardingActivity}. */
class OnboardingResources {
    private static final String EXTRA_ICON = "ICON_RES_ID";
    private static final String EXTRA_TITLE = "TITLE_RES_ID";
    private static final String EXTRA_MESSAGE = "MESSAGE_RES_ID";

    private static final ImmutableMap<String, OnboardingResources> RESOURCES_FOR_SCREEN =
            new ImmutableMap.Builder<String, OnboardingResources>()
                    .put(
                            ScreenKey.WELCOME,
                            new OnboardingResources(
                                    R.drawable.ic_launcher,
                                    R.string.app_name,
                                    R.string.onboarding_message_welcome))
                    .put(
                            ScreenKey.MOUSE,
                            new OnboardingResources(
                                    R.drawable.ic_ob_wrist_gesture,
                                    R.string.pref_inputMouse,
                                    R.string.onboarding_message_mouse))
                    .put(
                            ScreenKey.KEYPAD,
                            new OnboardingResources(
                                    R.drawable.ic_ob_keypad,
                                    R.string.pref_inputCursor,
                                    R.string.onboarding_message_keypad))
                    .build();

    @DrawableRes final int iconResId;
    @StringRes final int titleResId;
    @StringRes final int messageResId;

    private OnboardingResources(
            @DrawableRes int icon, @StringRes int title, @StringRes int message) {
        iconResId = icon;
        titleResId = title;
        messageResId = message;
    }

    /**
     * Generate an Intent to start a specified tutorial with a {@code startActivityForResult()}
     * call.
     *
     * @param context A Context of the application package for {@link OnboardingActivity }.
     * @return An Intent to pass into {@code startActivityForResult()}.
     */
    Intent toIntent(Context context) {
        return new Intent(context, OnboardingActivity.class)
                .putExtra(EXTRA_ICON, iconResId)
                .putExtra(EXTRA_TITLE, titleResId)
                .putExtra(EXTRA_MESSAGE, messageResId);
    }

    /**
     * Extract the resource IDs from an Intent that started {@link OnboardingActivity}.
     *
     * @param intent An Intent that launched this tutorial.
     * @return The resource IDs to display in the {@link OnboardingActivity}.
     */
    @Nullable
    static OnboardingResources fromIntent(Intent intent) {
        if (intent != null) {
            int icon = intent.getIntExtra(EXTRA_ICON, 0);
            int title = intent.getIntExtra(EXTRA_TITLE, 0);
            int message = intent.getIntExtra(EXTRA_MESSAGE, 0);

            if (icon != 0 && title != 0 && message != 0) {
                return new OnboardingResources(icon, title, message);
            }
        }

        return null;
    }

    /**
     * Generate an instance that contains the necessary resources for the specified tutorial.
     *
     * @param key Key of the corresponding tutorial screen.
     * @return The resource IDs to display in the {@link OnboardingActivity}.
     */
    static OnboardingResources forScreen(@ScreenKey String key) {
        return RESOURCES_FOR_SCREEN.get(key);
    }
}
