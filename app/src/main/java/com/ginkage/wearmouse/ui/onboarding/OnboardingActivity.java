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

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.ginkage.wearmouse.R;

/** Simple tutorial activity that shows an icon, some hints and a big round button. */
public class OnboardingActivity extends Activity {
    private static final String TAG = "OnboardingActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        setResult(RESULT_CANCELED, getIntent());

        OnboardingResources resources = OnboardingResources.fromIntent(getIntent());
        if (resources == null) {
            Log.e(TAG, "Failed to retrieve resources from the Intent: " + getIntent());
            finish();
            return;
        }

        ImageView headerIcon = findViewById(R.id.header);
        TextView title = findViewById(R.id.title);
        TextView message = findViewById(R.id.message);

        headerIcon.setBackgroundResource(resources.iconResId);
        title.setText(resources.titleResId);
        message.setText(resources.messageResId);

        findViewById(R.id.roundbutton_image).setOnClickListener(this::onClick);
    }

    private void onClick(View v) {
        setResult(RESULT_OK, getIntent());
        finish();
    }
}
