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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.MainThread;
import android.support.wearable.preference.WearablePreferenceActivity;
import android.util.Log;
import com.ginkage.wearmouse.R;
import com.ginkage.wearmouse.bluetooth.HidDataSender;
import com.ginkage.wearmouse.bluetooth.HidDeviceProfile;
import com.ginkage.wearmouse.ui.input.ModeSelectFragment;
import java.util.ArrayList;
import java.util.List;

/** Paired Bluetooth devices list. */
public class PairedDevicesFragment extends PreferenceFragment {
    private static final String TAG = "BluetoothSettings";

    private static final int PREFERENCE_ORDER_NORMAL = 100;

    private BluetoothAdapter bluetoothAdapter;
    private HidDeviceProfile hidDeviceProfile;
    private HidDataSender hidDataSender;

    private final List<Preference> bondedDevices = new ArrayList<>();

    private boolean scanReceiverRegistered;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
        try {
            addPreferencesFromResource(R.xml.prefs_paired_devices);
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }

        Context context = getContext();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        hidDataSender = HidDataSender.getInstance();
        hidDeviceProfile = hidDataSender.register(context, profileListener);

        context.registerReceiver(
                bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBluetoothStateAndDevices();
    }

    @Override
    public void onPause() {
        unregisterScanReceiver();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Context context = getContext();
        context.unregisterReceiver(bluetoothStateReceiver);
        hidDataSender.unregister(context, profileListener);
        context.stopService(new Intent(context, NotificationService.class));
        super.onDestroy();
    }

    protected void updateBluetoothStateAndDevices() {
        switch (bluetoothAdapter.getState()) {
            case BluetoothAdapter.STATE_OFF:
                unregisterScanReceiver();
                clearBondedDevices();
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
            case BluetoothAdapter.STATE_TURNING_OFF:
                clearBondedDevices();
                startActivity(new Intent(getActivity(), BluetoothStateActivity.class));
                break;
            case BluetoothAdapter.STATE_ON:
                registerScanReceiver();
                updateBondedDevices();
                break;
            default: // fall out
        }
    }

    protected void updateBondedDevices() {
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            updatePreferenceBondState(device);
        }
    }

    /** Examine the bond state of the device and update preference if necessary. */
    private void updatePreferenceBondState(final BluetoothDevice device) {
        final BluetoothDevicePreference pref = findOrAllocateDevicePreference(device);
        pref.updateBondState();
        pref.updateProfileConnectionState();
        switch (device.getBondState()) {
            case BluetoothDevice.BOND_BONDED:
                pref.setEnabled(true);
                pref.setOrder(PREFERENCE_ORDER_NORMAL);
                bondedDevices.add(pref);
                getPreferenceScreen().addPreference(pref);
                break;
            case BluetoothDevice.BOND_NONE:
                pref.setEnabled(false);
                bondedDevices.remove(pref);
                getPreferenceScreen().removePreference(pref);
                break;
            case BluetoothDevice.BOND_BONDING:
                pref.setEnabled(false);
                break;
            default: // fall out
        }
    }

    protected void clearBondedDevices() {
        for (Preference p : bondedDevices) {
            getPreferenceScreen().removePreference(p);
        }
        bondedDevices.clear();
    }

    private void registerScanReceiver() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getContext().registerReceiver(bluetoothScanReceiver, intentFilter);
        scanReceiverRegistered = true;

        BluetoothUtils.setScanMode(
                bluetoothAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 0);
    }

    private void unregisterScanReceiver() {
        if (scanReceiverRegistered) {
            getContext().unregisterReceiver(bluetoothScanReceiver);
            scanReceiverRegistered = false;
            BluetoothUtils.setScanMode(bluetoothAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE, 0);
        }
    }

    private final HidDataSender.ProfileListener profileListener =
            new HidDataSender.ProfileListener() {
                @Override
                @MainThread
                public void onServiceStateChanged(BluetoothProfile proxy) {
                    if (proxy != null) {
                        for (final BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                            final BluetoothDevicePreference pref = findDevicePreference(device);
                            if (pref != null) {
                                pref.updateProfileConnectionState();
                            }
                        }
                    }
                }

                @Override
                @MainThread
                public void onDeviceStateChanged(BluetoothDevice device, int state) {
                    updatePreferenceBondState(device);

                    Context context = getContext();
                    if (state == BluetoothProfile.STATE_DISCONNECTED) {
                        context.stopService(new Intent(context, NotificationService.class));
                    } else {
                        Intent intent = NotificationService.buildIntent(device.getName(), state);
                        intent.setClass(context, NotificationService.class);
                        context.startService(intent);
                    }

                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        ((WearablePreferenceActivity) getActivity())
                                .startPreferenceFragment(new ModeSelectFragment(), true);
                    }
                }

                @Override
                @MainThread
                public void onAppUnregistered() {
                    getActivity().finish();
                }
            };

    /** Handles bluetooth scan responses and other indicators. */
    private final BroadcastReceiver bluetoothScanReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (getContext() == null) {
                        Log.w(TAG, "BluetoothScanReceiver got intent with no context");
                        return;
                    }
                    final BluetoothDevice device =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    final String action = intent.getAction();
                    switch (action == null ? "" : action) {
                        case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                            updatePreferenceBondState(device);
                            break;
                        case BluetoothDevice.ACTION_NAME_CHANGED:
                            BluetoothDevicePreference pref = findDevicePreference(device);
                            if (pref != null) {
                                pref.updateName();
                            }
                            break;
                        default: // fall out
                    }
                }
            };

    /** Receiver to listen for changes in the bluetooth adapter state. */
    private final BroadcastReceiver bluetoothStateReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (getContext() == null) {
                        Log.w(TAG, "BluetoothStateReceiver got intent with no context");
                        return;
                    }
                    if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                        updateBluetoothStateAndDevices();
                    }
                }
            };

    /**
     * Looks for a preference in the preference group.
     *
     * <p>Returns null if no preference found.
     */
    private BluetoothDevicePreference findDevicePreference(final BluetoothDevice device) {
        return (BluetoothDevicePreference) findPreference(device.getAddress());
    }

    /**
     * Looks for a preference in the preference group.
     *
     * <p>Allocates a new preference if none found.
     */
    private BluetoothDevicePreference findOrAllocateDevicePreference(final BluetoothDevice device) {
        BluetoothDevicePreference pref = findDevicePreference(device);
        if (pref == null) {
            pref = new BluetoothDevicePreference(getContext(), device, hidDeviceProfile);
        }
        return pref;
    }
}
