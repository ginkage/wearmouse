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

package com.ginkage.wearmouse.sensors;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import javax.annotation.Nullable;

/** Helper class to manage the Sensor Service binding to an Activity. */
public class SensorServiceConnection {

    /** Interface for listening to the service binding event. */
    public interface ConnectionListener {
        /**
         * Callback that receives the service instance.
         *
         * @param service Service instance.
         */
        void onServiceConnected(SensorService service);
    }

    private final ServiceConnection connection =
            new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName className, IBinder service) {
                    if (service != null) {
                        SensorService.LocalBinder binder = (SensorService.LocalBinder) service;
                        SensorServiceConnection.this.service = binder.getService();
                        listener.onServiceConnected(SensorServiceConnection.this.service);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName className) {
                    service = null;
                }
            };

    private final ConnectionListener listener;
    private final Context context;

    @Nullable private SensorService service;
    private boolean bound;

    /**
     * @param context Activity that the service is bound to.
     * @param listener Callback to receive the service instance.
     */
    public SensorServiceConnection(Context context, ConnectionListener listener) {
        this.context = checkNotNull(context);
        this.listener = checkNotNull(listener);
    }

    /**
     * Gets the service object instance.
     *
     * @return Service instance.
     */
    @Nullable public SensorService getService() {
        return service;
    }

    /** Connects the activity to the service, starting it if required. */
    public void bind() {
        if (!bound) {
            Intent intent = new Intent(context, SensorService.class);
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
            bound = true;
        }
    }

    /** Unbinds the service from the activity. */
    public void unbind() {
        if (service != null) {
            service.stopInput();
            service = null;
        }
        if (bound) {
            context.unbindService(connection);
            bound = false;
        }
    }
}
