package com.ginkage.wearmouse.input;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class TouchpadGestureDetector {

    private static final String TAG = "TouchpadGesture";

    /** Callback for the detected gestures. */
    public interface GestureListener {
        void onLeftDown();

        void onRightDown();

        void onLeftUp();

        void onRightUp();

        void onMove(float dX, float dY);

        void onScroll(float dWheel);
    }

    private final GestureListener gestureListener;
    private final TouchpadGestureListener touchpadListener;

    /**
     * @param context Context for getting some device-specific measurements.
     * @param gestureListener Callback to listen for the detected gestures.
     */
    public TouchpadGestureDetector(Context context, GestureListener gestureListener) {
        this.gestureListener = checkNotNull(gestureListener);
        this.touchpadListener = new TouchpadGestureListener(context);
    }

    /** Should be called in the View's onTouchEvent() method. */
    public void onTouchEvent(MotionEvent e) {
        touchpadListener.onTouchEvent(e);
    }

    private class TouchpadGestureListener extends GestureDetector.SimpleOnGestureListener {

        private final GestureDetector gestureDetector;
        private final int slopSquare;
        private final int tapTimeout;

        private float lastX, lastY;
        private float downFocusX, downFocusY;
        private boolean alwaysInTapRegion;
        private boolean inDoubleTap;
        private boolean inRightClick;

        TouchpadGestureListener(Context context) {
            gestureDetector = new GestureDetector(context, this);
            gestureDetector.setIsLongpressEnabled(false);

            final ViewConfiguration configuration = ViewConfiguration.get(context);
            final int touchSlop = configuration.getScaledTouchSlop();
            slopSquare = touchSlop * touchSlop;
            tapTimeout = ViewConfiguration.getTapTimeout();
        }

        void onTouchEvent(MotionEvent e) {
            final int action = e.getActionMasked();
            final boolean pointerUp = action == MotionEvent.ACTION_POINTER_UP;
            final int skipIndex = pointerUp ? e.getActionIndex() : -1;
            float sumX = 0, sumY = 0;
            final int count = e.getPointerCount();
            for (int i = 0; i < count; i++) {
                if (skipIndex == i) continue;
                sumX += e.getX(i);
                sumY += e.getY(i);
            }
            final int div = pointerUp ? count - 1 : count;
            final float focusX = sumX / div;
            final float focusY = sumY / div;

            if (count > 1 && !pointerUp) {
                inRightClick = true;
            }

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    downFocusX = focusX;
                    downFocusY = focusY;
                    alwaysInTapRegion = true;
                    break;
                case MotionEvent.ACTION_MOVE:
                    {
                        if (alwaysInTapRegion) {
                            final int deltaX = (int) (focusX - downFocusX);
                            final int deltaY = (int) (focusY - downFocusY);
                            int distance = (deltaX * deltaX) + (deltaY * deltaY);
                            if (distance > slopSquare) {
                                alwaysInTapRegion = false;
                            }
                        }

                        if (inRightClick && !alwaysInTapRegion && count > 1) {
                            gestureListener.onScroll((lastY - focusY) / 10.0f);
                        } else {
                            gestureListener.onMove(focusX - lastX, focusY - lastY);
                        }
                    }
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    if (alwaysInTapRegion) {
                        press();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (inRightClick) {
                        release();
                    } else if (alwaysInTapRegion && !inDoubleTap) {
                        press();
                    }
                    break;
                default:
                    break;
            }
            lastX = focusX;
            lastY = focusY;

            gestureDetector.onTouchEvent(e);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            release();
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    inDoubleTap = true;
                    break;
                case MotionEvent.ACTION_UP:
                    inDoubleTap = false;
                    release();
                    if (alwaysInTapRegion && (e.getEventTime() - e.getDownTime()) <= tapTimeout) {
                        press();
                        release();
                    }
                    break;
                default:
                    break;
            }
            return false;
        }

        private void press() {
            if (inRightClick) {
                gestureListener.onRightDown();
            } else {
                gestureListener.onLeftDown();
            }
        }

        private void release() {
            if (inRightClick) {
                gestureListener.onRightUp();
                inRightClick = false;
            } else {
                gestureListener.onLeftUp();
            }
        }
    }
}
