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

import android.app.Fragment;
import android.os.Bundle;
import android.support.wearable.input.RotaryEncoder;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.ginkage.wearmouse.R;
import com.ginkage.wearmouse.input.TouchpadController;
import com.ginkage.wearmouse.input.TouchpadGestureDetector;

public class TouchpadFragment extends Fragment {

    private FrameLayout swipeDismissLayout;
    private TouchpadGestureDetector gestureDetector;
    private TouchpadGestureDetector.GestureListener gestureListener;
    private TouchpadController controller;
    private float scrollFactor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        controller = new TouchpadController(() -> getActivity().finish());
        gestureListener = controller.onCreate(getContext());
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_touchpad, container, false);

        scrollFactor = -ViewConfiguration.get(getContext()).getScaledVerticalScrollFactor() / 5.0f;

        gestureDetector = new TouchpadGestureDetector(getContext(), gestureListener);
        root.findViewById(R.id.container)
                .setOnTouchListener((view, motionEvent) -> onTouchEvent(motionEvent));

        swipeDismissLayout = getActivity().findViewById(android.R.id.content);

        return root;
    }

    @Override
    public void onDestroy() {
        controller.onDestroy(getContext());
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        getView().requestFocus();
    }

    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            // Prevent Swipe-To-Dismiss
            MotionEvent cancel = MotionEvent.obtain(e);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            swipeDismissLayout.onTouchEvent(cancel);
            cancel.recycle();
        }
        gestureDetector.onTouchEvent(e);
        return true;
    }

    public boolean onGenericMotionEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_SCROLL && RotaryEncoder.isFromRotaryEncoder(ev)) {
            controller.onRotaryInput(RotaryEncoder.getRotaryAxisValue(ev) * scrollFactor);
            return true;
        }
        return false;
    }
}
