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

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import com.ginkage.wearmouse.ui.onboarding.OnboardingController.ScreenKey;

/** Wrapper class that allows to launch a tutorial, check and store its completion state. */
public class OnboardingRequest {
    private final Activity activity;
    private final OnboardingController controller;
    @ScreenKey private final String key;

    public OnboardingRequest(Activity activity, @ScreenKey String key) {
        this.activity = checkNotNull(activity);
        this.controller = new OnboardingController(activity);
        this.key = checkNotNull(key);
    }

    /**
     * Gets the onboarding state of a specified tutorial.
     *
     * @return {@code true} if the specified tutorial was completed, {@code false} otherwise.
     */
    public boolean isComplete() {
        return controller.isComplete(key);
    }

    /** Marks the specified tutorial as completed. */
    public void setComplete() {
        controller.setComplete(key);
    }

    /** Starts the onboarding activity for the specified tutorial. */
    public void start() {
        activity.startActivityForResult(
                OnboardingResources.forScreen(key)
                        .toIntent(activity)
                        .putExtra(OnboardingController.EXTRA_KEY, key),
                OnboardingController.ONBOARDING_REQUEST_CODE);
    }

    /** Starts the onboarding activity for the specified tutorial from within a fragment. */
    public void start(Fragment fragment) {
        fragment.startActivityForResult(
                OnboardingResources.forScreen(key)
                        .toIntent(activity)
                        .putExtra(OnboardingController.EXTRA_KEY, key),
                OnboardingController.ONBOARDING_REQUEST_CODE);
    }

    /**
     * Checks if an onActivityResult() call corresponds to this onboarding request.
     *
     * @param requestCode Request code parameter from onActivityResult().
     * @param data Data intent parameter from onActivityResult().
     * @return {@code true} if this request corresponds to the onActivityResult() response, {@code
     *     false} otherwise.
     */
    public boolean isMyResult(int requestCode, Intent data) {
        if (requestCode == OnboardingController.ONBOARDING_REQUEST_CODE && data != null) {
            String intentKey = data.getStringExtra(OnboardingController.EXTRA_KEY);
            return (intentKey != null && TextUtils.equals(intentKey, key));
        }
        return false;
    }
}
