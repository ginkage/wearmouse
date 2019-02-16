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
import android.provider.Settings;
import androidx.annotation.MainThread;
import android.view.MotionEvent;
import android.view.Surface;
import com.ginkage.wearmouse.bluetooth.HidDataSender;
import com.ginkage.wearmouse.input.MouseSensorListener.HandMode;
import com.ginkage.wearmouse.input.MouseSensorListener.MouseButton;
import com.ginkage.wearmouse.sensors.SensorService;
import com.ginkage.wearmouse.sensors.SensorServiceConnection;

/** Controls the sensor-based Mouse input behaviour for the corresponding UI. */
public class MouseController {

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
    private final SettingsUtil settings;
    private final HidDataSender hidDataSender;
    private final MouseSensorListener sensorListener;
    private final SensorServiceConnection connection;

    /**
     * @param context Activity this controller is bound to.
     * @param ui Callback for receiving the UI updates.
     */
    public MouseController(Context context, Ui ui) {
        this.ui = checkNotNull(ui);
        this.settings = new SettingsUtil(context);
        this.hidDataSender = HidDataSender.getInstance();
        this.sensorListener = new MouseSensorListener(hidDataSender);
        this.connection = new SensorServiceConnection(context, this::onServiceConnected);
    }

    /**
     * Should be called in the Activity's (or Fragment's) onCreate() method.
     *
     * @param context The context to register listener with.
     */
    public void onCreate(Context context) {
        sensorListener.onCreate();
        hidDataSender.register(context, profileListener);
    }

    /** Should be called in the Activity's (or Fragment's) onStart() method. */
    public void onStart() {
        connection.bind();
    }

    /** Should be called in the Activity's (or Fragment's) onStop() method. */
    public void onStop() {
        connection.unbind();
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
     * Should be called from a Mouse Button View's OnTouchListener callback.
     *
     * @param leftButton {@code true} if the event came from the Left mouse button, {@code false} if
     *     it was from the right one.
     * @param event Touch event to react to.
     */
    public void onTouch(MotionEvent event, boolean leftButton) {
        final int action = event.getActionMasked();
        final @MouseButton int button =
                leftButton ? MouseSensorListener.BUTTON_LEFT : MouseSensorListener.BUTTON_RIGHT;
        if (action == MotionEvent.ACTION_DOWN) {
            sendButtonEvent(button, true);
        } else if (action == MotionEvent.ACTION_UP) {
            sendButtonEvent(button, false);
        }
    }

    /**
     * Should be called when an RSB event is detected.
     *
     * @param delta Movement of the Mouse Wheel.
     */
    public void onRotaryInput(float delta) {
        sensorListener.sendMouseMove(0, 0, delta);
    }

    /** Sends a Left Mouse Button "down" event. */
    public void leftClickAndHold() {
        sendButtonEvent(MouseSensorListener.BUTTON_LEFT, true);
    }

    /** Sends a Right Mouse Button "down" event. */
    public void rightClickAndHold() {
        sendButtonEvent(MouseSensorListener.BUTTON_RIGHT, true);
    }

    /** Sends a Middle Mouse Button "down" event immediately followed by an "up" event. */
    public void middleClick() {
        sendButtonEvent(MouseSensorListener.BUTTON_MIDDLE, true);
        sendButtonEvent(MouseSensorListener.BUTTON_MIDDLE, false);
    }

    /**
     * Sets the current watch location.
     *
     * @param hand Hand index: left wrist, center (in the hand) or right wrist.
     * @see MouseSensorListener
     */
    public void setMouseHand(@HandMode int hand) {
        settings.putMouseHand(hand);
        sensorListener.setHand(hand);
    }

    /**
     * Get the last used watch location from the last time.
     *
     * @return Hand index.
     * @see MouseSensorListener
     */
    public @HandMode int getMouseHand() {
        return settings.getMouseHand();
    }

    private void onServiceConnected(SensorService service) {
        sensorListener.setLefty(isLefty(service.getApplicationContext()));
        sensorListener.setHand(settings.getMouseHand());
        sensorListener.setStabilize(settings.getBoolean(SettingsUtil.STABILIZE));
        service.startInput(sensorListener, settings.getBoolean(SettingsUtil.REDUCED_RATE));
    }

    private boolean isLefty(Context context) {
        return Settings.System.getInt(
                        context.getContentResolver(),
                        Settings.System.USER_ROTATION,
                        Surface.ROTATION_0)
                == Surface.ROTATION_180;
    }

    private void sendButtonEvent(int button, boolean state) {
        sensorListener.sendButtonEvent(button, state);
    }
}
