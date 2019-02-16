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

import android.app.RemoteInput;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.MainThread;
import android.support.wearable.input.RemoteInputConstants;
import android.support.wearable.input.RemoteInputIntent;
import com.ginkage.wearmouse.bluetooth.HidDataSender;
import javax.annotation.Nullable;

/** Controls the Keyboard input behaviour for the corresponding UI. */
public class KeyboardInputController {

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
    private final KeyboardHelper keyboardHelper;

    /** @param ui Callback for receiving the UI updates. */
    public KeyboardInputController(Ui ui) {
        this.ui = checkNotNull(ui);
        this.hidDataSender = HidDataSender.getInstance();
        this.keyboardHelper = new KeyboardHelper(hidDataSender);
    }

    /**
     * Creates an Intent that should be called with {@code startActivityForResult} to request the
     * Remote Input data.
     *
     * @param pm Package Manager for checking if the intent can be resolved.
     * @return Intent for Remote Input or {@code null} if Remote Input is not supported.
     */
    public @Nullable Intent getInputIntent(PackageManager pm) {
        Bundle extras = new Bundle();
        extras.putBoolean(RemoteInputConstants.EXTRA_DISALLOW_EMOJI, true);
        final Intent inputIntent =
                new Intent(RemoteInputIntent.ACTION_REMOTE_INPUT)
                        .putExtra(
                                RemoteInputIntent.EXTRA_REMOTE_INPUTS,
                                new RemoteInput[] {
                                    new RemoteInput.Builder(Intent.EXTRA_TEXT)
                                            .setAllowFreeFormInput(true)
                                            .addExtras(extras)
                                            .build()
                                })
                        .putExtra(RemoteInputIntent.EXTRA_SKIP_CONFIRMATION_UI, true);

        if (pm.resolveActivity(inputIntent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
            return inputIntent;
        }

        return null;
    }

    /**
     * Should be called in the Activity's (or Fragment's) onCreate() method.
     *
     * @param context The context to register listener with.
     */
    public void onCreate(Context context) {
        hidDataSender.register(context, profileListener);
    }

    /** Should be called in the Activity's (or Fragment's) onResume() method. */
    public void onResume() {
        if (!hidDataSender.isConnected()) {
            ui.onDeviceDisconnected();
        }
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
     * Should be called in the Activity's (or Fragment's) onActivityResult() method that was invoked
     * in response to the Intent returned by {@code getInputIntent}.
     */
    public void onActivityResult(Intent data) {
        final Bundle result = RemoteInput.getResultsFromIntent(data);
        if (result != null) {
            CharSequence text = result.getCharSequence(Intent.EXTRA_TEXT);
            if (text != null) {
                for (int i = 0, n = text.length(); i < n; ++i) {
                    keyboardHelper.sendChar(text.charAt(i));
                }
            }
        }
    }
}
