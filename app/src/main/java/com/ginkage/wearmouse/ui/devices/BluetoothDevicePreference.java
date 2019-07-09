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

import static com.google.common.base.Preconditions.checkNotNull;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.support.wearable.preference.WearableDialogPreference;
import android.support.wearable.view.AcceptDenyDialog;
import android.text.TextUtils;
import android.util.Log;
import com.ginkage.wearmouse.R;
import com.ginkage.wearmouse.bluetooth.HidDataSender;
import com.ginkage.wearmouse.bluetooth.HidDeviceProfile;

/** Preference class representing a single bluetooth device. */
class BluetoothDevicePreference extends WearableDialogPreference {
    private static final String TAG = "BluetoothDevicePref";

    private final BluetoothDevice device;
    private final HidDeviceProfile hidDeviceProfile;
    private final HidDataSender hidDataSender;

    private int connectionState;

    BluetoothDevicePreference(
            Context context,
            final BluetoothDevice device,
            final HidDeviceProfile hidDeviceProfile) {
        super(context);
        this.device = checkNotNull(device);
        this.hidDeviceProfile = checkNotNull(hidDeviceProfile);
        this.hidDataSender = HidDataSender.getInstance();

        setKey(this.device.getAddress());
        setIcon(R.drawable.ic_bt_bluetooth);

        updateName();
        updateBondState();
        updateClass();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        connectionState = hidDeviceProfile.getConnectionState(device);
        if (connectionState == BluetoothProfile.STATE_CONNECTED
                || connectionState == BluetoothProfile.STATE_CONNECTING) {
            builder.setPositiveButton(
                    R.string.pref_bluetooth_disconnect,
                    (dialog, which) -> hidDataSender.requestConnect(null));
        } else {
            builder.setPositiveButton(
                    R.string.pref_bluetooth_connect,
                    (dialog, which) -> hidDataSender.requestConnect(device));
        }

        if (connectionState == BluetoothProfile.STATE_CONNECTED) {
            builder.setNegativeButton(
                    R.string.pref_bluetooth_select,
                    (dialog, which) -> hidDataSender.requestConnect(device));
        } else if (device.getBluetoothClass() != null
                && device.getBluetoothClass().getMajorDeviceClass()
                        != BluetoothClass.Device.Major.PHONE) {
            builder.setNegativeButton(
                    R.string.pref_bluetooth_forget, (dialog, which) -> requestUnpair());
        }
    }

    /** Present when the device is available */
    @Override
    protected void onClick() {
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            // No need to try to connect to devices when we don't support any profiles
            // on the target device.
            if (hidDeviceProfile.isProfileSupported(device)) {
                super.onClick();
            }
        } else {
            // Discovery may be in progress so cancel discovery before attempting to bond.
            stopDiscovery();
            device.createBond();
        }
    }

    /** Request to unpair and remove the bond */
    private void requestUnpair() {
        final int state = device.getBondState();

        if (state == BluetoothDevice.BOND_BONDING) {
            BluetoothUtils.cancelBondProcess(device);
        } else if (state != BluetoothDevice.BOND_NONE) {
            AcceptDenyDialog diag = new AcceptDenyDialog(getContext());
            diag.setTitle(R.string.pref_bluetooth_unpair);
            diag.setMessage(device.getName());
            diag.setPositiveButton(
                    (dialog, which) -> {
                        if (!BluetoothUtils.removeBond(device)) {
                            Log.w(TAG, "Unpair request rejected straight away.");
                        }
                    });
            diag.setNegativeButton((dialog, which) -> {});
            diag.show();
        }
    }

    private void stopDiscovery() {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            if (adapter.isDiscovering()) {
                adapter.cancelDiscovery();
            }
        }
    }

    void updateName() {
        String name = device.getName();
        if (TextUtils.isEmpty(name)) {
            name = device.getAddress();
        }

        setTitle(name);
        setDialogTitle(name);
        notifyChanged();
    }

    /** Re-examine the device and update the bond state. */
    void updateBondState() {
        switch (device.getBondState()) {
            case BluetoothDevice.BOND_BONDED:
                setSummary(null);
                break;
            case BluetoothDevice.BOND_BONDING:
                setSummary(R.string.pref_bluetooth_pairing);
                break;
            case BluetoothDevice.BOND_NONE:
                setSummary(R.string.pref_bluetooth_available);
                break;
            default: // fall out
        }
        notifyChanged();
    }

    private void updateClass() {
        if (device.getBluetoothClass() == null) {
            return;
        }

        switch (device.getBluetoothClass().getDeviceClass()) {
            case BluetoothClass.Device.PHONE_CELLULAR:
            case BluetoothClass.Device.PHONE_SMART:
            case BluetoothClass.Device.PHONE_UNCATEGORIZED:
                setIcon(R.drawable.ic_bt_phone);
                break;

            case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES:
            case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET:
            case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER:
                setIcon(R.drawable.ic_bt_headset);
                break;

            case BluetoothClass.Device.WEARABLE_WRIST_WATCH:
                setIcon(R.drawable.ic_bt_watch);
                break;

            case BluetoothClass.Device.WEARABLE_GLASSES:
                setIcon(R.drawable.ic_bt_glass);
                break;
            default: // fall out
        }
        notifyChanged();
    }

    /**
     * Update the preference summary with the profile connection state
     *
     * <p>However, if no profiles are supported from the target device we indicate that this target
     * device is unavailable.
     */
    void updateProfileConnectionState() {
        connectionState = hidDeviceProfile.getConnectionState(device);
        if (!hidDeviceProfile.isProfileSupported(device)) {
            setEnabled(false);
            setSummary(R.string.pref_bluetooth_unavailable);
        } else {
            switch (connectionState) {
                case BluetoothProfile.STATE_CONNECTED:
                    setEnabled(true);
                    setSummary(R.string.pref_bluetooth_connected);
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    setEnabled(false);
                    setSummary(R.string.pref_bluetooth_connecting);
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    setEnabled(false);
                    setSummary(R.string.pref_bluetooth_disconnecting);
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    setEnabled(true);
                    setSummary(R.string.pref_bluetooth_disconnected);
                    break;
                default: // fall out
            }
        }
        notifyChanged();
    }
}
