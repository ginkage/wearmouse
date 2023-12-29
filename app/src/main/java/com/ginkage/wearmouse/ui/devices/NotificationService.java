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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import com.ginkage.wearmouse.R;
import com.ginkage.wearmouse.bluetooth.HidDataSender;
import javax.annotation.Nullable;

/**
 * Service that shows an ongoing notification if there is a device connected. Android P requires
 * that for HID profile app to remain registered, it should be "visible", i.e. have a running
 * activity or a foreground service. We don't want to become unregistered with an active connection,
 * so we'll display a notification to stay "visible".
 */
public class NotificationService extends Service {
    private static final int ONGOING_NOTIFICATION_ID = 0x1111;
    private static final String NOTIFICATION_CHANNEL_ID = "WearMouseNotif";
    private static final String NOTIFICATION_CHANNEL_NAME = "All notifications";
    private static final String ACTION_START = "com.ginkage.wearmouse.START_NOTIF";
    private static final String EXTRA_DEVICE = "EXTRA_DEVICE";
    private static final String EXTRA_STATE = "EXTRA_STATE";

    private boolean isForeground;
    private NotificationManager notificationManager;
    private HidDataSender hidDataSender;

    private final HidDataSender.ProfileListener profileListener =
            new HidDataSender.ProfileListener() {
                @Override
                public void onConnectionStateChanged(BluetoothDevice device, int state) {
                    if (state == BluetoothProfile.STATE_DISCONNECTED) {
                        stopSelf();
                    }
                }

                @Override
                public void onAppStatusChanged(boolean registered) {}

                @Override
                public void onServiceStateChanged(BluetoothProfile proxy) {}
            };

    private final NotificationChannel notificationChannel =
            new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_START.equals(intent.getAction()) && hidDataSender.isConnected()) {
            String device = intent.getStringExtra(EXTRA_DEVICE);
            int state = intent.getIntExtra(EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED);
            updateNotification(device, state);
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        hidDataSender = HidDataSender.getInstance();
        hidDataSender.register(this, profileListener);

        notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    @Override
    public void onDestroy() {
        if (isForeground) {
            stopForeground(true);
            isForeground = false;
        }

        hidDataSender.unregister(this, profileListener);

        super.onDestroy();
    }

    /**
     * Returns an intent that should be passed to startService() to create or update the ongoing
     * notification.
     *
     * @param device Name of the device we are connected to, if any (may be {@code null})
     * @param state Device connection state
     * @return Intent that will update the notification text
     */
    public static Intent buildIntent(String device, int state) {
        return new Intent(ACTION_START).putExtra(EXTRA_DEVICE, device).putExtra(EXTRA_STATE, state);
    }

    private void updateNotification(String device, int state) {
        String text = getStateName(state);
        if (!TextUtils.isEmpty(device)) {
            text += ": " + device;
        }

        Notification notification = buildNotification(text);

        if (isForeground) {
            if (notificationManager != null) {
                notificationManager.notify(ONGOING_NOTIFICATION_ID, notification);
            }
        } else {
            startForeground(ONGOING_NOTIFICATION_ID, notification);
            isForeground = true;
        }
    }

    private Notification buildNotification(String text) {
        Intent intent =
                new Intent(this, WelcomeActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setAction(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setLocalOnly(true)
                .setOngoing(true)
                .setSmallIcon(getApplicationInfo().icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .build();
    }

    private String getStateName(int state) {
        switch (state) {
            case BluetoothProfile.STATE_CONNECTED:
                return getString(R.string.pref_bluetooth_connected);
            case BluetoothProfile.STATE_CONNECTING:
                return getString(R.string.pref_bluetooth_connecting);
            case BluetoothProfile.STATE_DISCONNECTING:
                return getString(R.string.pref_bluetooth_disconnecting);
            case BluetoothProfile.STATE_DISCONNECTED:
                return getString(R.string.pref_bluetooth_disconnected);
            default:
                return getString(R.string.pref_bluetooth_unavailable);
        }
    }
}
