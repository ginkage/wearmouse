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
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;
import android.support.wearable.input.RotaryEncoder;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.ginkage.wearmouse.R;
import com.ginkage.wearmouse.input.MouseController;
import com.ginkage.wearmouse.input.MouseSensorListener;
import com.ginkage.wearmouse.ui.onboarding.OnboardingController;
import com.ginkage.wearmouse.ui.onboarding.OnboardingRequest;
import java.util.ArrayList;
import java.util.List;

/** The Air Mouse UI fragment. */
public class MouseFragment extends Fragment {
    private static final float WATCH_EDGE_SIZE_RATIO = .2f;

    private WearableActionDrawerView actionDrawer;
    private WearableNavigationDrawerView navigationDrawer;
    private ImageView pointerImage;
    private FrameLayout swipeDismissLayout;
    private FrameLayout mouseHint;
    private int edgeSize;
    private float scrollFactor;
    private MouseController controller;
    private OnboardingRequest onboardingRequest;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getContext();
        controller = new MouseController(context, () -> getActivity().finish());
        controller.onCreate(context);

        onboardingRequest = new OnboardingRequest(getActivity(), OnboardingController.OB_MOUSE);
        if (!onboardingRequest.isComplete()) {
            onboardingRequest.start(this);
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_mouse, container, false);

        pointerImage = root.findViewById(R.id.pointer_direction);
        setPointerRotation(controller.getMouseHand());

        edgeSize = getResources().getDimensionPixelSize(R.dimen.ws_drawer_view_edge_size);
        scrollFactor = -ViewConfiguration.get(getContext()).getScaledVerticalScrollFactor() / 5.0f;

        navigationDrawer = root.findViewById(R.id.top_navigation_drawer);
        navigationDrawer.setAdapter(new HandPagerAdapter());
        navigationDrawer.addOnItemSelectedListener(
                i -> {
                    setPointerRotation(i);
                    controller.setMouseHand(i);
                });
        navigationDrawer.setCurrentItem(controller.getMouseHand(), false);

        actionDrawer = root.findViewById(R.id.bottom_action_drawer);
        actionDrawer.setOnMenuItemClickListener(this::onMenuItemClick);

        mouseHint = root.findViewById(R.id.mouse_hint);
        mouseHint.setOnTouchListener(this::onTouchHint);
        root.findViewById(R.id.content_frame).setOnTouchListener(this::onTouchContent);
        root.findViewById(R.id.left_button).setOnTouchListener(this::onTouchButton);
        root.findViewById(R.id.right_button).setOnTouchListener(this::onTouchButton);

        swipeDismissLayout = getActivity().findViewById(android.R.id.content);

        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (onboardingRequest.isMyResult(requestCode, data) && resultCode == Activity.RESULT_OK) {
            mouseHint.setVisibility(View.VISIBLE);
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

    private boolean onTouchContent(View v, MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            final float y = event.getY();
            final int edgeSizePx =
                    Math.min(edgeSize, (int) (WATCH_EDGE_SIZE_RATIO * v.getHeight()));
            if (y < v.getTop() + edgeSizePx) {
                navigationDrawer.getController().peekDrawer();
            }
            if (y > v.getBottom() - edgeSizePx) {
                actionDrawer.getController().peekDrawer();
            }
        }
        return true;
    }

    private boolean onTouchButton(View v, MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            // Prevent Swipe-To-Dismiss on buttons
            MotionEvent cancel = MotionEvent.obtain(event);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            swipeDismissLayout.onTouchEvent(cancel);
            cancel.recycle();
        }
        controller.onTouch(event, v.getId() == R.id.left_button);
        return true;
    }

    private boolean onMenuItemClick(MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.menu_click_hold) {
            controller.leftClickAndHold();
            actionDrawer.getController().peekDrawer();
        } else if (id == R.id.menu_right_click) {
            controller.rightClickAndHold();
            actionDrawer.getController().peekDrawer();
        } else if (id == R.id.menu_middle_click) {
            controller.middleClick();
        }
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
        controller.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        navigationDrawer.getController().peekDrawer();
        actionDrawer.getController().peekDrawer();
    }

    @Override
    public void onStop() {
        controller.onStop();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        controller.onDestroy(getContext());
        super.onDestroy();
    }

    public boolean onGenericMotionEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_SCROLL && RotaryEncoder.isFromRotaryEncoder(ev)) {
            controller.onRotaryInput(RotaryEncoder.getRotaryAxisValue(ev) * scrollFactor);
            return true;
        }
        return false;
    }

    private void setPointerRotation(int hand) {
        switch (hand) {
            case MouseSensorListener.HAND_LEFT:
                pointerImage.setRotation(90);
                break;
            case MouseSensorListener.HAND_CENTER:
                pointerImage.setRotation(0);
                break;
            case MouseSensorListener.HAND_RIGHT:
                pointerImage.setRotation(-90);
                break;
            default:
                break;
        }
    }

    private class HandPagerAdapter
            extends WearableNavigationDrawerView.WearableNavigationDrawerAdapter {
        private final List<String> handList;

        HandPagerAdapter() {
            handList = new ArrayList<>();
            handList.add(getString(R.string.mouse_hand_left));
            handList.add(getString(R.string.mouse_hand_center));
            handList.add(getString(R.string.mouse_hand_right));
        }

        @Override
        public String getItemText(int i) {
            return handList.get(i);
        }

        @Override
        public Drawable getItemDrawable(int i) {
            Context context = getContext();
            switch (i) {
                case MouseSensorListener.HAND_LEFT:
                    return context.getDrawable(R.drawable.ic_am_hand_left);
                case MouseSensorListener.HAND_CENTER:
                    return context.getDrawable(R.drawable.ic_am_hand_center);
                case MouseSensorListener.HAND_RIGHT:
                    return context.getDrawable(R.drawable.ic_am_hand_right);
                default:
                    break;
            }
            return null;
        }

        @Override
        public int getCount() {
            return handList.size();
        }
    }
}
