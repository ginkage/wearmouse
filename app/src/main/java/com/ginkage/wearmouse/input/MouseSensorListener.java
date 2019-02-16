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

import androidx.annotation.IntDef;
import com.ginkage.wearmouse.bluetooth.MouseReport.MouseDataSender;
import com.ginkage.wearmouse.sensors.SensorService;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/** Helper class that interprets sensor data and translates it to Mouse data events. */
public class MouseSensorListener implements SensorService.OrientationListener {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({HAND_LEFT, HAND_CENTER, HAND_RIGHT})
    @interface HandMode {}

    public static final int HAND_LEFT = 0;
    public static final int HAND_CENTER = 1;
    public static final int HAND_RIGHT = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE})
    @interface MouseButton {}

    public static final int BUTTON_LEFT = 0;
    public static final int BUTTON_RIGHT = 1;
    public static final int BUTTON_MIDDLE = 2;

    private static final double CURSOR_SPEED = 1024.0 / (Math.PI / 4);
    private static final double STABILIZE_BIAS = 16.0;

    static final class ButtonEvent {
        final @MouseButton int button;
        final boolean state;

        ButtonEvent(int b, boolean s) {
            button = b;
            state = s;
        }
    }

    /**
     * A list of button events that are pending and need to be sent. The oldest event is at the
     * front.
     */
    private final List<ButtonEvent> pendingEvents = new ArrayList<>();

    private final MouseDataSender dataSender;

    private double yaw;
    private double pitch;
    private double dYaw;
    private double dPitch;
    private double dWheel;

    /**
     * Whether this is the very first event we received after starting to listen or changing the
     * wrist mode.
     */
    private boolean firstRead;

    private boolean leftButtonPressed;
    private boolean rightButtonPressed;
    private boolean middleButtonPressed;
    private @HandMode int handMode;
    private boolean stabilize;
    private boolean lefty;

    /** @param dataSender Interface to send Mouse data with. */
    MouseSensorListener(MouseDataSender dataSender) {
        this.dataSender = checkNotNull(dataSender);
    }

    @Override
    public void onOrientation(float[] quaternion) {
        double q1 = quaternion[0]; // X * sin(T/2)
        double q2 = quaternion[1]; // Y * sin(T/2)
        double q3 = quaternion[2]; // Z * sin(T/2)
        double q0 = quaternion[3]; // cos(T/2)

        if (lefty) {
            // Rotate 180 degrees
            q1 = -q1;
            q2 = -q2;
        }

        if (handMode == HAND_LEFT) {
            // Rotate 90 degrees counter-clockwise
            double x = q1;
            double y = q2;
            q1 = -y;
            q2 = x;
        } else if (handMode == HAND_RIGHT) {
            // Rotate 90 degrees clockwise
            double x = q1;
            double y = q2;
            q1 = y;
            q2 = -x;
        } // else it's HAND_CENTER for which we do not need to rotate.

        double yaw = Math.atan2(2 * (q0 * q3 - q1 * q2), (1 - 2 * (q1 * q1 + q3 * q3)));
        double pitch = Math.asin(2 * (q0 * q1 + q2 * q3));
        // double roll = Math.atan2(2 * (q0 * q2 - q1 * q3), (1 - 2 * (q1 * q1 + q2 * q2)));

        if (Double.isNaN(yaw) || Double.isNaN(pitch)) {
            // NaN case, skip it
            return;
        }

        if (firstRead) {
            this.yaw = yaw;
            this.pitch = pitch;
            firstRead = false;
        } else {
            final double newYaw = highpass(this.yaw, yaw);
            final double newPitch = highpass(this.pitch, pitch);

            double dYaw = clamp(this.yaw - newYaw);
            double dPitch = this.pitch - newPitch;
            this.yaw = newYaw;
            this.pitch = newPitch;

            // Accumulate the error locally.
            this.dYaw += dYaw;
            this.dPitch += dPitch;
        }

        sendCurrentState();
    }

    /** Should be called in the controller's onCreate() method. */
    void onCreate() {
        pendingEvents.clear();
        firstRead = true;
        yaw = 0;
        pitch = 0;
        dYaw = 0;
        dPitch = 0;
    }

    /**
     * Enqueue a button press event.
     *
     * @param button Button index. Can be one of BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE.
     * @param state {@code true} if the button is pressed, {@code false} otherwise.
     */
    void sendButtonEvent(@MouseButton int button, boolean state) {
        synchronized (pendingEvents) {
            pendingEvents.add(new ButtonEvent(button, state));
        }
    }

    /**
     * Adjust the enqueued Mouse data with an offset.
     *
     * @param x Extra displacement along X axis.
     * @param y Extra displacement along Y axis.
     * @param wheel Extra Wheel rotation.
     */
    void sendMouseMove(double x, double y, double wheel) {
        dYaw += x / CURSOR_SPEED;
        dPitch += y / CURSOR_SPEED;
        dWheel += wheel;
    }

    /**
     * Sets the current watch location: left/right wrist, or in the hand.
     *
     * @param hand Can be one of HAND_LEFT, HAND_CENTER, HAND_RIGHT.
     */
    void setHand(@HandMode int hand) {
        handMode = hand;
        firstRead = true;
    }

    /**
     * Set the pointer stabilization setting state.
     *
     * @param stabilize {@code true} if pointer stabilization is enabled, {@code false} otherwise.
     */
    void setStabilize(boolean stabilize) {
        this.stabilize = stabilize;
    }

    /**
     * Sets the "lefty" mode for the mouse data. This inverts all movements along Y axis.
     *
     * @param isLefty {@code true} if "lefty" mode is active, {@code false} if not.
     */
    void setLefty(boolean isLefty) {
        lefty = isLefty;
    }

    private static double clamp(double val) {
        while (val <= -Math.PI) {
            val += 2 * Math.PI;
        }
        while (val >= Math.PI) {
            val -= 2 * Math.PI;
        }
        return val;
    }

    /**
     * Applies an adaptive high-pass filter if mStability is {@code true}. Otherwise simple returns
     * the new value.
     */
    private double highpass(double oldVal, double newVal) {
        if (!stabilize) {
            return newVal;
        }
        double delta = clamp(oldVal - newVal);
        double alpha =
                Math.max(0, 1 - Math.pow(Math.abs(delta) * CURSOR_SPEED / STABILIZE_BIAS, 3));
        return newVal + alpha * delta;
    }

    /**
     * Returns {@code true} if we couldn't send the full displacement in one go (if it didn't fit in
     * one byte), {@code false} otherwise.
     */
    private boolean sendCurrentState() {
        boolean overflow = false;
        double dX = dYaw * CURSOR_SPEED;
        double dY = dPitch * CURSOR_SPEED;
        double dZ = dWheel;

        // Scale the shift down to fit the protocol.
        if (dX > 127) {
            dY *= 127.0 / dX;
            dX = 127;
            overflow = true;
        }
        if (dX < -127) {
            dY *= -127.0 / dX;
            dX = -127;
            overflow = true;
        }
        if (dY > 127) {
            dX *= 127.0 / dY;
            dY = 127;
            overflow = true;
        }
        if (dY < -127) {
            dX *= -127.0 / dY;
            dY = -127;
            overflow = true;
        }
        if (dZ > 127) {
            dZ = 127;
            overflow = true;
        }
        if (dZ < -127) {
            dZ = -127;
            overflow = true;
        }

        final byte x = (byte) Math.round(dX);
        final byte y = (byte) Math.round(dY);
        final byte z = (byte) Math.round(dZ);
        sendData(x, y, z);

        // Only subtract the part of the error that was already sent.
        if (x != 0) {
            dYaw -= x / CURSOR_SPEED;
        }
        if (y != 0) {
            dPitch -= y / CURSOR_SPEED;
        }
        if (z != 0) {
            dWheel -= z;
        }

        return overflow;
    }

    private void sendData(byte x, byte y, byte wheel) {
        synchronized (pendingEvents) {
            if (!pendingEvents.isEmpty()) {
                ButtonEvent event = pendingEvents.remove(0);
                if (event.button == BUTTON_LEFT) {
                    leftButtonPressed = event.state;
                } else if (event.button == BUTTON_RIGHT) {
                    rightButtonPressed = event.state;
                } else if (event.button == BUTTON_MIDDLE) {
                    middleButtonPressed = event.state;
                }
            }
        }

        dataSender.sendMouse(
                leftButtonPressed, rightButtonPressed, middleButtonPressed, x, y, wheel);
    }
}
