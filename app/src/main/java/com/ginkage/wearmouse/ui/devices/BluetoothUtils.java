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
import android.util.Log;
import java.lang.reflect.Method;
import javax.annotation.Nullable;

/** Helper class that exposes some hidden methods from the Android framework. */
class BluetoothUtils {
    private static final String TAG = "BluetoothUtils";
    private static final Method methodCancelBondProcess = lookupCancelBondProcess();
    private static final Method methodRemoveBond = lookupRemoveBond();
    private static final Method methodSetScanMode = lookupSetScanMode();

    /** Cancel an in-progress bonding request started with {@link #createBond}. */
    static boolean cancelBondProcess(BluetoothDevice device) {
        if (methodCancelBondProcess != null) {
            try {
                return (Boolean) methodCancelBondProcess.invoke(device);
            } catch (Exception e) {
                Log.e(TAG, "Error invoking cancelBondProcess", e);
            }
        }
        return false;
    }

    /**
     * Remove bond (pairing) with the remote device.
     *
     * <p>Delete the link key associated with the remote device, and immediately terminate
     * connections to that device that require authentication and encryption.
     */
    static boolean removeBond(BluetoothDevice device) {
        if (methodRemoveBond != null) {
            try {
                return (Boolean) methodRemoveBond.invoke(device);
            } catch (Exception e) {
                Log.e(TAG, "Error invoking removeBond", e);
            }
        }
        return false;
    }

    /**
     * Set the Bluetooth scan mode of the local Bluetooth adapter.
     *
     * <p>The Bluetooth scan mode determines if the local adapter is connectable and/or discoverable
     * from remote Bluetooth devices.
     *
     * <p>For privacy reasons, discoverable mode is automatically turned off after <code>duration
     * </code> seconds. For example, 120 seconds should be enough for a remote device to initiate
     * and complete its discovery process.
     */
    static boolean setScanMode(BluetoothAdapter adapter, int mode, int duration) {
        if (methodSetScanMode != null) {
            try {
                return (Boolean) methodSetScanMode.invoke(adapter, mode, duration);
            } catch (Exception e) {
                Log.e(TAG, "Error invoking setScanMode", e);
            }
        }
        return false;
    }

    @Nullable
    private static Method lookupCancelBondProcess() {
        try {
            return BluetoothDevice.class.getMethod("cancelBondProcess");
        } catch (Exception e) {
            Log.e(TAG, "Error looking up cancelBondProcess", e);
        }
        return null;
    }

    @Nullable
    private static Method lookupRemoveBond() {
        try {
            return BluetoothDevice.class.getMethod("removeBond");
        } catch (Exception e) {
            Log.e(TAG, "Error looking up removeBond", e);
        }
        return null;
    }

    @Nullable
    private static Method lookupSetScanMode() {
        try {
            return BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
        } catch (Exception e) {
            Log.e(TAG, "Error looking up setScanMode", e);
        }
        return null;
    }
}
