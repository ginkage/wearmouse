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
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.ginkage.wearmouse.R;
import com.ginkage.wearmouse.input.KeyboardInputController;

/** Implements a "card-flip" animation using custom fragment transactions. */
public class InputActivity extends Activity {
    public static final String EXTRA_INPUT_MODE = "input_mode";
    public static final int MODE_MOUSE = 1;
    public static final int MODE_KEYPAD = 2;

    private static final int INPUT_REQUEST_CODE = 1;

    private KeyboardInputController keyboardController;
    private boolean keypadMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_flip);

        keyboardController = new KeyboardInputController(this::finish);
        keyboardController.onCreate(this);

        keypadMode = false;
        Intent intent = getIntent();
        if (intent != null) {
            int mode = intent.getIntExtra(EXTRA_INPUT_MODE, -1);
            if (mode > 0) {
                keypadMode = mode == MODE_KEYPAD;
            }
        }

        getFragmentManager()
                .beginTransaction()
                .add(
                        R.id.fragment_container,
                        keypadMode ? new KeypadFragment() : new MouseFragment())
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
                flipCard();
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
        return (!keypadMode && ((MouseFragment) getFragment()).onGenericMotionEvent(ev))
                || super.onGenericMotionEvent(ev);
    }

    private Fragment getFragment() {
        return getFragmentManager().findFragmentById(R.id.fragment_container);
    }

    private void flipCard() {
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.animator.card_flip_right_in, R.animator.card_flip_right_out)
                .replace(
                        R.id.fragment_container,
                        keypadMode ? new MouseFragment() : new KeypadFragment())
                .commit();

        keypadMode = !keypadMode;
    }
}
