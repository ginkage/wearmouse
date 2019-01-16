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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout.LayoutParams;
import android.support.wearable.view.DismissOverlayView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.ginkage.wearmouse.R;
import com.ginkage.wearmouse.input.KeyboardHelper;
import com.ginkage.wearmouse.input.KeypadController;
import com.ginkage.wearmouse.input.KeypadGestureDetector;
import com.ginkage.wearmouse.ui.onboarding.OnboardingController;
import com.ginkage.wearmouse.ui.onboarding.OnboardingRequest;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/** The Cursor Keypad UI fragment. */
public class KeypadFragment extends Fragment {
    private ImageView pointerView;
    private ImageView crossView;
    private LayoutParams defaultLayout;
    private LayoutParams layoutParams;
    private TextView swipeName;
    private FrameLayout keypadHint;
    private FrameLayout swipeDismissLayout;
    private DismissOverlayView dismissOverlay;
    private KeypadGestureDetector gestureDetector;
    private KeypadGestureDetector.GestureListener gestureListener;
    private KeypadController controller;
    private OnboardingRequest onboardingRequest;
    private int frameWidth;
    private int frameHeight;
    private int pointerWidth;
    private int pointerHeight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getContext();
        controller = new KeypadController(context, new KeypadUi(), new DefaultKeyNameProvider());
        gestureListener = controller.onCreate(context);

        onboardingRequest = new OnboardingRequest(getActivity(), OnboardingController.OB_KEYPAD);
        if (!onboardingRequest.isComplete()) {
            onboardingRequest.start(this);
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_keypad, container, false);

        root.findViewById(R.id.container)
                .addOnLayoutChangeListener(
                        (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                            if (gestureDetector == null) {
                                frameWidth = v.getMeasuredWidth();
                                frameHeight = v.getMeasuredHeight();
                                gestureDetector =
                                        new KeypadGestureDetector(
                                                getContext(),
                                                gestureListener,
                                                frameWidth,
                                                frameHeight);
                                defaultLayout = (LayoutParams) pointerView.getLayoutParams();
                                layoutParams = new LayoutParams(defaultLayout);
                                pointerWidth = pointerView.getMeasuredWidth();
                                pointerHeight = pointerView.getMeasuredHeight();
                                v.setOnTouchListener(
                                        (view, motionEvent) -> onTouchEvent(motionEvent));
                            }
                        });

        swipeName = root.findViewById(R.id.swipe_name);
        pointerView = root.findViewById(R.id.pointer_image);
        crossView = root.findViewById(R.id.cross);

        keypadHint = root.findViewById(R.id.keypad_hint);
        keypadHint.setOnTouchListener(this::onTouchHint);

        dismissOverlay = root.findViewById(R.id.dismiss_overlay);

        swipeDismissLayout = getActivity().findViewById(android.R.id.content);

        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (onboardingRequest.isMyResult(requestCode, data) && resultCode == Activity.RESULT_OK) {
            keypadHint.setVisibility(View.VISIBLE);
            onboardingRequest.setComplete();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean onTouchHint(View v, MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            v.setVisibility(View.GONE);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        controller.onResume();
    }

    @Override
    public void onDestroy() {
        controller.onDestroy(getContext());
        super.onDestroy();
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

    private class KeypadUi implements KeypadController.Ui {
        @Override
        public void showUsageHint() {
            keypadHint.setVisibility(View.VISIBLE);
        }

        @Override
        public void showDismissOverlay() {
            dismissOverlay.show();
        }

        @Override
        public void setPointerPosition(float x, float y) {
            layoutParams.horizontalBias = (x - pointerWidth * 0.5f) / (frameWidth - pointerWidth);
            layoutParams.verticalBias = (y - pointerHeight * 0.5f) / (frameHeight - pointerHeight);
            pointerView.setLayoutParams(layoutParams);
        }

        @Override
        public void resetPointerPosition() {
            pointerView.setLayoutParams(defaultLayout);
        }

        @Override
        public void setCenterText(String text, boolean is8Way) {
            swipeName.setText(text);
            crossView.setImageDrawable(
                    getResources()
                            .getDrawable(
                                    is8Way
                                            ? R.drawable.kp_cross_8_way
                                            : R.drawable.kp_cross_4_way));
        }

        @Override
        public void onDeviceDisconnected() {
            getActivity().finish();
        }
    }

    private class DefaultKeyNameProvider implements KeypadController.KeyNameProvider {
        private final Map<Integer, String> swipeName =
                new ImmutableMap.Builder<Integer, String>()
                        .put(KeyboardHelper.KEY_ESCAPE, getString(R.string.key_name_escape))
                        .put(KeyboardHelper.KEY_BACKSPACE, getString(R.string.key_name_backspace))
                        .put(KeyboardHelper.KEY_TAB, getString(R.string.key_name_tab))
                        .put(KeyboardHelper.KEY_SPACE, getString(R.string.key_name_space))
                        .build();

        @Override
        public String getKeyName(int scanCode) {
            return swipeName.get(scanCode);
        }
    }
}
