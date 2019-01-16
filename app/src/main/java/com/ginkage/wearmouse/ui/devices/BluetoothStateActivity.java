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

package com.ginkage.wearmouse.ui.devices;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;
import com.ginkage.wearmouse.R;

/** Show a spinner animation while the Bluetooth is turning on or off. */
public class BluetoothStateActivity extends WearableActivity {

    private final BroadcastReceiver bluetoothStateReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                        checkState(
                                intent.getIntExtra(
                                        BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR));
                    }
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAmbientEnabled();
        setContentView(R.layout.activity_bt_state);
        registerReceiver(
                bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        checkState(BluetoothAdapter.getDefaultAdapter().getState());
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(bluetoothStateReceiver);
        super.onDestroy();
    }

    private void checkState(int state) {
        if (state != BluetoothAdapter.STATE_TURNING_ON
                && state != BluetoothAdapter.STATE_TURNING_OFF) {
            finish();
        } else {
            ((TextView) findViewById(R.id.title))
                    .setText(
                            getString(
                                    state == BluetoothAdapter.STATE_TURNING_ON
                                            ? R.string.pref_bluetooth_turningOn
                                            : R.string.pref_bluetooth_turningOff));
        }
    }
}
