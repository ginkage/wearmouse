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
import android.support.annotation.IntDef;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.wear.ambient.AmbientModeSupport;
import android.support.wear.ambient.AmbientModeSupport.AmbientCallback;
import android.support.wear.ambient.AmbientModeSupport.AmbientCallbackProvider;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.ginkage.wearmouse.R;
import com.ginkage.wearmouse.input.KeyboardInputController;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Implements a "card-flip" animation using custom fragment transactions. */
public class InputActivity extends FragmentActivity implements AmbientCallbackProvider {
    public static final String EXTRA_INPUT_MODE = "input_mode";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MODE_MOUSE, MODE_KEYPAD, MODE_TOUCHPAD})
    @interface InputMode {}

    public static final int MODE_MOUSE = 1;
    public static final int MODE_KEYPAD = 2;
    public static final int MODE_TOUCHPAD = 3;

    private static final int INPUT_REQUEST_CODE = 1;

    private KeyboardInputController keyboardController;
    private @InputMode int currentMode;

    @Override
    public AmbientCallback getAmbientCallback() {
        return new AmbientCallback() {
            @Override
            public void onEnterAmbient(Bundle ambientDetails) {
                super.onEnterAmbient(ambientDetails);
                finish();
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_flip);
        AmbientModeSupport.attach(this);

        keyboardController = new KeyboardInputController(this::finish);
        keyboardController.onCreate(this);

        currentMode = MODE_MOUSE;
        Intent intent = getIntent();
        if (intent != null) {
            int mode = intent.getIntExtra(EXTRA_INPUT_MODE, -1);
            if (mode > 0) {
                currentMode = mode;
            }
        }

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, getFragment(currentMode))
                .commit();
    }

    @Override
    protected void onDestroy() {
        keyboardController.onDestroy(this);
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int action = event.getAction();
        if (keyCode == KeyEvent.KEYCODE_STEM_1) {
            if (action == KeyEvent.ACTION_UP) {
                flipCard(
                        currentMode == MODE_MOUSE
                                ? MODE_TOUCHPAD
                                : currentMode == MODE_TOUCHPAD ? MODE_KEYPAD : MODE_MOUSE);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_STEM_2) {
            if (action == KeyEvent.ACTION_UP) {
                Intent intent = keyboardController.getInputIntent(getPackageManager());
                if (intent != null) {
                    startActivityForResult(intent, INPUT_REQUEST_CODE);
                }
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == INPUT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            keyboardController.onActivityResult(data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent ev) {
        return ((currentMode == MODE_MOUSE
                                && ((MouseFragment) getFragment()).onGenericMotionEvent(ev))
                        || (currentMode == MODE_TOUCHPAD
                                && ((TouchpadFragment) getFragment()).onGenericMotionEvent(ev)))
                || super.onGenericMotionEvent(ev);
    }

    private Fragment getFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    private void flipCard(@InputMode int mode) {
        currentMode = mode;
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.animator.card_flip_right_in, R.animator.card_flip_right_out)
                .replace(R.id.fragment_container, getFragment(currentMode))
                .commit();
    }

    private Fragment getFragment(@InputMode int mode) {
        return mode == MODE_KEYPAD
                ? new KeypadFragment()
                : mode == MODE_MOUSE ? new MouseFragment() : new TouchpadFragment();
    }
}
