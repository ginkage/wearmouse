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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import androidx.annotation.MainThread;
import com.ginkage.wearmouse.bluetooth.HidDataSender;
import com.ginkage.wearmouse.input.KeyboardHelper.Key;
import com.ginkage.wearmouse.input.SettingsUtil.SettingKey;

/** Controls the 4-way or 8-way cursor Keypad input behaviour for the corresponding UI. */
public class KeypadController {

    private static final int CENTER_AREA = -1;
    private static final int NONE = 0;

    // The keypad's outer circle can be imagined as divided into 16 equal sectors (each one having a
    // size of pi/8), numbered counter-clockwise from 0 to 15, each one corresponding to either a
    // sector of the 4-way cursor, or 8-way, or both. Arrays sFourWay and sEightWay are indexed with
    // the sector number and contain indices in sPressKey and sSwipeKey arrays that contain the key
    // (or keys) that will be sent when that area is pressed (or swiped into).

    // In both 4-way and 8-way modes, this key will be pressed.
    private static final int[] fourWay = {3, 3, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3};

    // In the 8-way mode, this key will be pressed as well.
    private static final int[] eightWay = {-1, 0, 3, -1, -1, 1, 0, -1, -1, 2, 1, -1, -1, 3, 2, -1};

    // Zones are mapped in the order "top", "left", "bottom", "right".
    private static final int[] pressKey = {Key.UP, Key.LEFT, Key.DOWN, Key.RIGHT};

    // Key to press on a swipe from center.
    private static final int[] swipeKey = {Key.ESCAPE, Key.BACKSPACE, Key.TAB, Key.SPACE};

    /** Callback for the UI. */
    public interface Ui {
        /** Display the hint on how to use the Keypad mode. */
        void showUsageHint();

        /** Show the big red "press to exit" button. */
        void showDismissOverlay();

        /** Set the "joystick" pointer position to the specified coordinates. */
        void setPointerPosition(float x, float y);

        /** Reset the "joystick" pointer position to center. */
        void resetPointerPosition();

        /**
         * Update the UI, adding the text label in the keypad center, and switching between 4-way
         * and 8-way styles.
         *
         * @param text Text label to display in the center of the keypad.
         * @param is8Way {@code true} if the UI should show 8-way keypad, {@code false} if 4-way.
         */
        void setCenterText(String text, boolean is8Way);

        /** Called when the connection with the current device has been lost. */
        void onDeviceDisconnected();
    }

    /** Interface for providing the localized key names. */
    public interface KeyNameProvider {

        /**
         * Get the localized name for the specified key.
         *
         * @param scanCode The key we need a name for.
         * @return The localized key name.
         */
        String getKeyName(int scanCode);
    }

    private final int[] keyState = {NONE, NONE};

    private final HidDataSender.ProfileListener profileListener =
            new HidDataSender.ProfileListener() {
                @Override
                @MainThread
                public void onConnectionStateChanged(BluetoothDevice device, int state) {
                    if (state == BluetoothProfile.STATE_DISCONNECTED) {
                        ui.onDeviceDisconnected();
                    }
                }

                @Override
                @MainThread
                public void onAppStatusChanged(boolean registered) {
                    if (!registered) {
                        ui.onDeviceDisconnected();
                    }
                }

                @Override
                @MainThread
                public void onServiceStateChanged(BluetoothProfile proxy) {}
            };

    private final Ui ui;
    private final KeyNameProvider keyNameProvider;
    private final SettingsUtil settings;
    private final HidDataSender hidDataSender;
    private final KeyboardHelper keyboardHelper;

    private int touchArea;
    private int swipeArea;
    private boolean m8Way;

    /**
     * @param context The context to retrieve shared preferences with.
     * @param ui Callback for receiving the UI updates.
     * @param keyNameProvider Provider of the localized key names.
     */
    public KeypadController(Context context, Ui ui, KeyNameProvider keyNameProvider) {
        this.ui = checkNotNull(ui);
        this.keyNameProvider = checkNotNull(keyNameProvider);
        this.settings = new SettingsUtil(context);
        this.hidDataSender = HidDataSender.getInstance();
        this.keyboardHelper = new KeyboardHelper(hidDataSender);
    }

    /**
     * Should be called in the Activity's (or Fragment's) onCreate() method.
     *
     * @param context The context to register listener with.
     * @return A gesture listener that should be passed to the {@link KeypadGestureDetector}
     *     constructor.
     */
    public KeypadGestureDetector.GestureListener onCreate(Context context) {
        hidDataSender.register(context, profileListener);
        return new KeypadGestureListener();
    }

    /** Should be called in the Activity's (or Fragment's) onResume() method. */
    public void onResume() {
        m8Way = settings.getBoolean(SettingKey.CURSOR_8_WAY);
        ui.setCenterText("", m8Way);
        touchArea = CENTER_AREA;
        swipeArea = CENTER_AREA;
        sendKeyState();
    }

    /**
     * Should be called in the Activity's (or Fragment's) onDestroy() method.
     *
     * @param context The context to unregister listener with.
     */
    public void onDestroy(Context context) {
        hidDataSender.unregister(context, profileListener);
    }

    /**
     * Should be called when an RSB event is detected.
     *
     * @param delta Movement of the Mouse Wheel.
     */
    public void onRotaryInput(float delta) {
        hidDataSender.sendMouse(false, false, false, 0, 0, (int) delta);
    }

    private int getSwipeKey() {
        return swipeKey[fourWay[swipeArea]];
    }

    private void sendKeyPress(int key) {
        keyboardHelper.sendKeyDown(0, key);
        keyboardHelper.sendKeysUp(0);
    }

    private void sendKeyState() {
        int index = 0;

        if (touchArea >= 0) {
            keyState[index++] = pressKey[fourWay[touchArea]];
            if (m8Way && eightWay[touchArea] >= 0) {
                keyState[index++] = pressKey[eightWay[touchArea]];
            }
        }

        while (index < keyState.length) {
            keyState[index++] = NONE;
        }

        keyboardHelper.sendKeysDown(0, keyState[0], keyState[1]);
    }

    private class KeypadGestureListener implements KeypadGestureDetector.GestureListener {
        @Override
        public void onCenterLongPress() {
            ui.showDismissOverlay();
        }

        @Override
        public void onCenterSingleTap() {
            ui.showUsageHint();
        }

        @Override
        public void onCenterDoubleTap() {
            sendKeyPress(Key.ENTER);
        }

        @Override
        public void onAreaTouchDown(float x, float y, int area) {
            if (area != touchArea && swipeArea == CENTER_AREA) {
                touchArea = area;
                sendKeyState();
            }
            ui.setPointerPosition(x, y);
        }

        @Override
        public void onAreaSwipe(int area) {
            if (area != swipeArea) {
                swipeArea = area;
                // When a swipe in the center area is detected, keypad forcefully switches to the
                // 4-way cursor.
                ui.setCenterText(
                        area == CENTER_AREA ? "" : keyNameProvider.getKeyName(getSwipeKey()),
                        m8Way && area == CENTER_AREA);
            }
        }

        @Override
        public void onTouchUp() {
            if (swipeArea != CENTER_AREA) {
                sendKeyPress(getSwipeKey());
            } else {
                touchArea = CENTER_AREA;
                sendKeyState();
            }
            ui.setCenterText("", m8Way);
            ui.resetPointerPosition();
        }
    }
}
