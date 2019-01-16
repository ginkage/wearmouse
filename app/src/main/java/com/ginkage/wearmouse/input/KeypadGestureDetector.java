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
import android.view.GestureDetector;
import android.view.MotionEvent;

/** Detects key presses and swipes on 4-way or 8-way cursor Keypad. */
public class KeypadGestureDetector {

    /** Callback for the detected gestures. */
    public interface GestureListener {
        /** Called when a long-press on the center button is detected. */
        void onCenterLongPress();

        /** Called when a single tap on the center button is detected. */
        void onCenterSingleTap();

        /** Called when a double-tap on the center button is detected. */
        void onCenterDoubleTap();

        /**
         * Called when the user pressed down in one of the areas, or moved the pointer. This method
         * is never called when the pointer is on the center button.
         *
         * @param x X coordinate of the pointer's top-left corner.
         * @param y Y coordinate of the pointer's top-left corner.
         * @param area One of 16 shared areas that corresponds to this pointer location.
         */
        void onAreaTouchDown(float x, float y, int area);

        /**
         * Called to inform the UI about the ongoing swipe gesture (that is, when the user pressed
         * down in the center and moved the pointer outside).
         *
         * @param area One of 16 shared areas that corresponds to this pointer location, or -1 if
         *     the current pointer location does not correspond to a finished swipe.
         */
        void onAreaSwipe(int area);

        /** Called when the user is not pressing the Keypad anymore. */
        void onTouchUp();
    }

    private final GestureDetector gestureDetector;
    private final GestureListener gestureListener;
    private final int centerX;
    private final int centerY;
    private final float centerSize;
    private final float maxDist;

    private boolean fromCenter;

    /**
     * @param context Context for getting some device-specific measurements.
     * @param gestureListener Callback to listen for the detected gestures.
     * @param width Width of the Keypad View.
     * @param height Height of the Keypad View.
     */
    public KeypadGestureDetector(
            Context context, GestureListener gestureListener, int width, int height) {
        this.gestureListener = checkNotNull(gestureListener);
        centerX = width / 2;
        centerY = height / 2;

        float radius = Math.min(centerX, centerY);
        centerSize = radius * 0.4f;
        maxDist = radius * 0.8f;

        gestureDetector = new GestureDetector(context, new KeypadGestureListener());
    }

    /** Should be called in the View's onTouchEvent() method. */
    public void onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                fromCenter = isInCenter(e);
                if (!fromCenter) {
                    gestureListener.onAreaSwipe(-1);
                }
                // fall through
            case MotionEvent.ACTION_MOVE:
                onTouchDown(e.getX(), e.getY());
                break;

            case MotionEvent.ACTION_UP:
                fromCenter = false;
                onTouchUp();
                break;
            default: // fall out
        }
        gestureDetector.onTouchEvent(e);
    }

    private void onTouchDown(float x, float y) {
        final float diffX = x - centerX;
        final float diffY = y - centerY;
        final float dist = calcDistFromCenter(x, y);
        if (isInCenter(dist)) {
            if (fromCenter) {
                gestureListener.onAreaSwipe(-1);
            }
            onTouchUp();
            return;
        } else if (fromCenter) {
            gestureListener.onAreaSwipe(calcArea(x, y));
        }

        if (dist > maxDist) {
            x = centerX + (diffX / dist) * maxDist;
            y = centerY + (diffY / dist) * maxDist;
        }

        gestureListener.onAreaTouchDown(x, y, calcArea(x, y));
    }

    private void onTouchUp() {
        gestureListener.onTouchUp();
    }

    private boolean isInCenter(float dist) {
        return dist <= centerSize;
    }

    private boolean isInCenter(MotionEvent e) {
        return isInCenter(calcDistFromCenter(e.getX(), e.getY()));
    }

    private float calcDistFromCenter(float x, float y) {
        float diffX = x - centerX;
        float diffY = y - centerY;
        return (float) Math.hypot(diffX, diffY);
    }

    // Calculates the area index based on the angle, mapping it to one of 16 segments.
    private int calcArea(float x, float y) {
        float diffX = x - centerX;
        float diffY = y - centerY;
        double theta = Math.atan2(-diffY, diffX);
        if (theta < 0) {
            theta += 2 * Math.PI;
        }
        return (int) (theta / (Math.PI / 8));
    }

    private class KeypadGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(MotionEvent e) {
            if (isInCenter(e)) {
                gestureListener.onCenterLongPress();
            }
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (isInCenter(e)) {
                gestureListener.onCenterSingleTap();
                return true;
            }
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (isInCenter(e)) {
                gestureListener.onCenterDoubleTap();
                return true;
            }
            return false;
        }
    }
}
