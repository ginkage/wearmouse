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
import com.ginkage.wearmouse.input.MouseSensorListener.ButtonEvent;
import com.ginkage.wearmouse.input.MouseSensorListener.MouseButton;
import com.ginkage.wearmouse.input.SettingsUtil.SettingKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

public class TouchpadController {

    private static final String TAG = "TouchpadController";

    private static final int DATA_RATE_LOW_US = 20000;
    private static final int DATA_RATE_HIGH_US = 11250;

    /** Callback for the UI. */
    public interface Ui {
        /** Called when the connection with the current device has been lost. */
        void onDeviceDisconnected();
    }

    private final HidDataSender.ProfileListener profileListener =
            new HidDataSender.ProfileListener() {
                @Override
                @MainThread
                public void onDeviceStateChanged(BluetoothDevice device, int state) {
                    if (state == BluetoothProfile.STATE_DISCONNECTED) {
                        ui.onDeviceDisconnected();
                    }
                }

                @Override
                @MainThread
                public void onAppUnregistered() {
                    ui.onDeviceDisconnected();
                }

                @Override
                @MainThread
                public void onServiceStateChanged(BluetoothProfile proxy) {}
            };

    private final Ui ui;
    private final HidDataSender hidDataSender;
    private final List<ButtonEvent> pendingEvents = new ArrayList<>();

    private float dX;
    private float dY;
    private float dWheel;
    private boolean leftButton;
    private boolean rightButton;

    @Nullable private ScheduledFuture<?> scheduledFuture;
    private ScheduledThreadPoolExecutor executor;

    /**
     * @param ui Callback for receiving the UI updates.
     */
    public TouchpadController(Ui ui) {
        this.ui = checkNotNull(ui);
        this.hidDataSender = HidDataSender.getInstance();
    }

    /**
     * Should be called in the Activity's (or Fragment's) onCreate() method.
     *
     * @param context The context to register listener with.
     * @return A gesture listener that should be passed to the {@link TouchpadGestureDetector}
     *     constructor.
     */
    public TouchpadGestureDetector.GestureListener onCreate(Context context) {
        hidDataSender.register(context, profileListener);

        if (scheduledFuture == null) {
            boolean reducedRate = new SettingsUtil(context).getBoolean(SettingKey.REDUCED_RATE);
            long samplingPeriodUs = reducedRate ? DATA_RATE_LOW_US : DATA_RATE_HIGH_US;
            executor = new ScheduledThreadPoolExecutor(1);
            scheduledFuture =
                    executor.scheduleAtFixedRate(
                            this::sendData, 0, samplingPeriodUs, TimeUnit.MICROSECONDS);
        }

        return new TouchpadGestureListener();
    }

    /**
     * Should be called in the Activity's (or Fragment's) onDestroy() method.
     *
     * @param context The context to unregister listener with.
     */
    public void onDestroy(Context context) {
        if (scheduledFuture != null) {
            executor.shutdownNow();
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }

        hidDataSender.unregister(context, profileListener);
    }

    /**
     * Should be called when an RSB event is detected.
     *
     * @param delta Movement of the Mouse Wheel.
     */
    public void onRotaryInput(float delta) {
        synchronized (pendingEvents) {
            dWheel += delta;
        }
    }

    private class TouchpadGestureListener implements TouchpadGestureDetector.GestureListener {
        @Override
        public void onLeftDown() {
            sendButtonEvent(MouseButton.LEFT, true);
        }

        @Override
        public void onRightDown() {
            sendButtonEvent(MouseButton.RIGHT, true);
        }

        @Override
        public void onLeftUp() {
            sendButtonEvent(MouseButton.LEFT, false);
        }

        @Override
        public void onRightUp() {
            sendButtonEvent(MouseButton.RIGHT, false);
        }

        @Override
        public void onMove(float x, float y) {
            synchronized (pendingEvents) {
                dX += x;
                dY += y;
            }
        }

        @Override
        public void onScroll(float wheel) {
            synchronized (pendingEvents) {
                dWheel += wheel;
            }
        }

        private void sendButtonEvent(@MouseButton int button, boolean state) {
            synchronized (pendingEvents) {
                // Looks like one event is not enough
                ButtonEvent event = new ButtonEvent(button, state);
                pendingEvents.add(event);
                pendingEvents.add(event);
            }
        }
    }

    private void sendData() {
        int x, y, wheel;
        synchronized (pendingEvents) {
            x = (int) dX;
            y = (int) dY;
            wheel = (int) dWheel;
            dX -= x;
            dY -= y;
            dWheel -= wheel;

            if (!pendingEvents.isEmpty()) {
                ButtonEvent event = pendingEvents.remove(0);
                if (event.button == MouseButton.LEFT) {
                    leftButton = event.state;
                } else {
                    rightButton = event.state;
                }
            }
        }

        hidDataSender.sendMouse(leftButton, rightButton, false, x, y, wheel);
    }
}
